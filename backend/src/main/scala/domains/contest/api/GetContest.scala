package domains.contest.api

import cats.effect.IO
import domains.auth.api.AuthenticatedApi
import domains.auth.objects.internal.AuthenticatedUser
import domains.contest.objects.ContestSlug
import domains.contest.objects.response.{ContestDetail, ContestRegistrationStatus}
import domains.contest.table.contest.ContestTable
import domains.contest.utils.ContestAccessRules
import domains.usergroup.api.ListUserGroupSlugsForMember
import io.circe.Encoder
import org.http4s.{Method, Request, Status}
import shared.api.{ApiMessages, ApiPath, HttpApiError, PathParams}

import java.sql.Connection
import java.time.Instant

/** 获取比赛详情的认证 API，按注册、时间窗口和访问策略决定是否返回题目列表。 */
object GetContest extends AuthenticatedApi[ContestSlug, ContestDetail]:

  override val method: Method = Method.GET
  override val path: ApiPath = ApiPath("/api/contests/:contestSlug")
  override val successStatus: Status = Status.Ok
  override protected val outputEncoder: Encoder[ContestDetail] = summon[Encoder[ContestDetail]]

  /** 从路径解析比赛 slug，非法 slug 转为 400。 */
  override def decode(request: Request[IO], pathParams: PathParams): IO[ContestSlug] =
    val _ = request
    HttpApiError.fromEitherBadRequest(pathParams.require("contestSlug").flatMap(ContestSlug.parse))

  /** 加载比赛和调用者分组，未满足详情可见条件时按隐藏资源处理为 404。 */
  override def plan(
    connection: Connection,
    actor: AuthenticatedUser,
    slug: ContestSlug
  ): IO[ContestDetail] =
    for
      maybeContest <- ContestTable.findBySlug(connection, slug)
      contest <- maybeContest match
        case Some(contest) => IO.pure(contest)
        case None => HttpApiError.raise(HttpApiError.notFound(ApiMessages.contestNotFound))
      actorGroupSlugs <- ListUserGroupSlugsForMember.plan(connection, actor.username)
      isRegistered <- ContestTable.isRegistered(connection, contest.id, actor.username)
      now = Instant.now()
      /** 注意：无权查看详情返回 404，是隐藏受限比赛详情和题目清单的边界策略。 */
      _ <- HttpApiError.ensure(
        ContestAccessRules.canViewContestDetail(actor, contest, actorGroupSlugs.slugs.toSet, isRegistered, now),
        HttpApiError.notFound(ApiMessages.contestNotFound)
      )
      canManage = ContestAccessRules.canManageContest(actor, contest, actorGroupSlugs.slugs.toSet)
    yield ContestDetail.fromContest(
      contest,
      if isRegistered then ContestRegistrationStatus.registered else ContestRegistrationStatus.notRegistered,
      canManage,
      includeProblems = true
    )
