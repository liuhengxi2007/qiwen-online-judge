package domains.contest.api

import cats.effect.IO
import domains.auth.api.AuthenticatedApi
import domains.auth.objects.internal.AuthenticatedUser
import domains.contest.objects.ContestSlug
import domains.contest.objects.response.ContestRegistrationStatus
import domains.contest.table.contest.ContestTable
import domains.contest.table.contest.ContestTable.RegisterTableResult
import domains.contest.utils.ContestAccessRules
import domains.usergroup.api.ListUserGroupSlugsForMember
import io.circe.Encoder
import org.http4s.{Method, Request, Status}
import shared.api.{ApiMessages, ApiPath, HttpApiError, PathParams}

import java.sql.Connection
import java.time.Instant

/** 比赛报名认证 API，要求调用者可见比赛且当前时间早于开赛时间。 */
object RegisterContest extends AuthenticatedApi[ContestSlug, ContestRegistrationStatus]:

  override val method: Method = Method.POST
  override val path: ApiPath = ApiPath("/api/contests/:contestSlug/register")
  override val successStatus: Status = Status.Ok
  override protected val outputEncoder: Encoder[ContestRegistrationStatus] = summon[Encoder[ContestRegistrationStatus]]

  /** 从路径解析比赛 slug，报名入口不读取请求体。 */
  override def decode(request: Request[IO], pathParams: PathParams): IO[ContestSlug] =
    val _ = request
    HttpApiError.fromEitherBadRequest(pathParams.require("contestSlug").flatMap(ContestSlug.parse))

  /** 校验比赛可见性和报名时间，写入报名记录；重复报名返回冲突。 */
  override def plan(
    connection: Connection,
    actor: AuthenticatedUser,
    slug: ContestSlug
  ): IO[ContestRegistrationStatus] =
    for
      maybeContest <- ContestTable.findBySlug(connection, slug)
      contest <- maybeContest match
        case Some(contest) => IO.pure(contest)
        case None => HttpApiError.raise(HttpApiError.notFound(ApiMessages.contestNotFound))
      actorGroupSlugs <- ListUserGroupSlugsForMember.plan(connection, actor.username)
      /** 注意：不可见比赛返回 404，用于隐藏受限比赛是否存在。 */
      _ <- HttpApiError.ensure(
        ContestAccessRules.canViewContest(actor, contest, actorGroupSlugs.slugs.toSet),
        HttpApiError.notFound(ApiMessages.contestNotFound)
      )
      _ <- HttpApiError.ensure(
        Instant.now().isBefore(contest.startAt),
        HttpApiError.badRequest(ApiMessages.contestRegistrationClosed)
      )
      _ <- ContestTable.register(connection, contest.id, actor.username).flatMap {
        case RegisterTableResult.Registered => IO.unit
        case RegisterTableResult.AlreadyRegistered =>
          HttpApiError.raise(HttpApiError.conflict(ApiMessages.contestAlreadyRegistered))
      }
    yield ContestRegistrationStatus.registered
