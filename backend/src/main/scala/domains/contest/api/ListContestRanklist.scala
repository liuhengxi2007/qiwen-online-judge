package domains.contest.api

import cats.effect.IO
import domains.auth.api.AuthenticatedApi
import domains.auth.objects.internal.AuthenticatedUser
import domains.contest.objects.ContestSlug
import domains.contest.objects.response.ContestRanklistItem
import domains.contest.table.contest.{ContestRanklistTable, ContestTable}
import domains.contest.utils.ContestAccessRules
import domains.usergroup.api.ListUserGroupSlugsForMember
import io.circe.Encoder
import org.http4s.{Method, Request, Status}
import shared.api.utils.PageRequestQuerySupport
import shared.api.{ApiMessages, ApiPath, HttpApiError, PathParams}
import shared.objects.{PageRequest, PageResponse}

import java.sql.Connection
import java.time.Instant

/** 分页读取比赛榜单的认证 API，只有可查看比赛详情的用户可访问。 */
object ListContestRanklist extends AuthenticatedApi[(ContestSlug, PageRequest), PageResponse[ContestRanklistItem]]:

  override val method: Method = Method.GET
  override val path: ApiPath = ApiPath("/api/contests/:contestSlug/ranklist")
  override val successStatus: Status = Status.Ok
  override protected val outputEncoder: Encoder[PageResponse[ContestRanklistItem]] = summon[Encoder[PageResponse[ContestRanklistItem]]]

  /** 从路径解析比赛 slug，并从查询参数解析分页请求。 */
  override def decode(request: Request[IO], pathParams: PathParams): IO[(ContestSlug, PageRequest)] =
    for
      contestSlug <- HttpApiError.fromEitherBadRequest(pathParams.require("contestSlug").flatMap(ContestSlug.parse))
      pageRequest <- HttpApiError.fromEitherBadRequest(PageRequestQuerySupport.parsePageRequest(request.uri.query.params))
    yield (contestSlug, pageRequest)

  /** 校验详情可见性后读取榜单；比赛管理员可看到所有提交详情，普通用户只看自己的提交详情。 */
  override def plan(
    connection: Connection,
    actor: AuthenticatedUser,
    input: (ContestSlug, PageRequest)
  ): IO[PageResponse[ContestRanklistItem]] =
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
      /** 注意：无权查看榜单返回 404，与比赛详情的隐藏策略保持一致。 */
      _ <- HttpApiError.ensure(
        ContestAccessRules.canViewContestDetail(actor, contest, actorGroupSlugs.slugs.toSet, isRegistered, now),
        HttpApiError.notFound(ApiMessages.contestNotFound)
      )
      canManageContest = ContestAccessRules.canManageContest(actor, contest, actorGroupSlugs.slugs.toSet)
      ranklist <- ContestRanklistTable.listForContest(
        connection = connection,
        contestId = contest.id,
        viewerUsername = actor.username,
        canViewAllSubmissionDetails = canManageContest,
        page = normalizedPageRequest.page,
        pageSize = normalizedPageRequest.pageSize
      )
    yield ranklist
