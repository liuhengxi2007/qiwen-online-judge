package domains.contest.api

import cats.effect.IO
import domains.auth.api.AuthenticatedApi
import domains.auth.objects.internal.AuthenticatedUser
import domains.contest.objects.ContestSlug
import domains.contest.objects.response.ContestRegistrant
import domains.contest.table.contest.ContestTable
import domains.contest.utils.ContestAccessRules
import domains.usergroup.api.ListUserGroupSlugsForMember
import io.circe.Encoder
import org.http4s.{Method, Request, Status}
import shared.api.utils.PageRequestQuerySupport
import shared.api.{ApiMessages, ApiPath, HttpApiError, PathParams}
import shared.objects.{PageRequest, PageResponse}

import java.sql.Connection
import java.time.Instant

/** 分页读取比赛报名用户的认证 API，沿用比赛详情可见性作为边界。 */
object ListContestRegistrants extends AuthenticatedApi[(ContestSlug, PageRequest), PageResponse[ContestRegistrant]]:

  override val method: Method = Method.GET
  override val path: ApiPath = ApiPath("/api/contests/:contestSlug/registrants")
  override val successStatus: Status = Status.Ok
  override protected val outputEncoder: Encoder[PageResponse[ContestRegistrant]] = summon[Encoder[PageResponse[ContestRegistrant]]]

  /** 从路径解析比赛 slug，并从查询参数解析分页请求。 */
  override def decode(request: Request[IO], pathParams: PathParams): IO[(ContestSlug, PageRequest)] =
    for
      contestSlug <- HttpApiError.fromEitherBadRequest(pathParams.require("contestSlug").flatMap(ContestSlug.parse))
      pageRequest = PageRequestQuerySupport.parsePageRequest(request.uri.query.params)
    yield (contestSlug, pageRequest)

  /** 校验比赛详情可见性后读取报名列表，未授权时不暴露比赛存在。 */
  override def plan(
    connection: Connection,
    actor: AuthenticatedUser,
    input: (ContestSlug, PageRequest)
  ): IO[PageResponse[ContestRegistrant]] =
    val (contestSlug, pageRequest) = input
    val normalizedPageRequest = pageRequest.normalized
    for
      maybeContest <- ContestTable.findBySlug(connection, contestSlug)
      contest <- maybeContest match
        case Some(contest) => IO.pure(contest)
        case None => HttpApiError.raise(HttpApiError.notFound(ApiMessages.contestNotFound))
      actorGroupSlugs <- ListUserGroupSlugsForMember.plan(connection, actor.username)
      isRegistered <- ContestTable.isRegistered(connection, contest.id, actor.username)
      now = Instant.now()
      /** 注意：无权查看报名列表返回 404，与比赛详情隐藏策略保持一致。 */
      _ <- HttpApiError.ensure(
        ContestAccessRules.canViewContestDetail(actor, contest, actorGroupSlugs.slugs.toSet, isRegistered, now),
        HttpApiError.notFound(ApiMessages.contestNotFound)
      )
      registrants <- ContestTable.listRegistrants(connection, contest.id, normalizedPageRequest.page, normalizedPageRequest.pageSize)
    yield registrants
