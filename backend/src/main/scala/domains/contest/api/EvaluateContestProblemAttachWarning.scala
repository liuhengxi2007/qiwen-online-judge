package domains.contest.api

import cats.effect.IO
import domains.auth.api.AuthenticatedApi
import domains.auth.objects.internal.AuthenticatedUser
import domains.contest.objects.ContestSlug
import domains.contest.objects.response.ContestProblemAttachWarningResponse
import domains.contest.table.contest.{ContestProblemVisibilityTable, ContestTable}
import domains.contest.utils.ContestAccessRules
import domains.problem.api.EvaluateProblemAccess
import domains.problem.objects.ProblemSlug
import domains.usergroup.api.ListUserGroupSlugsForMember
import io.circe.Encoder
import org.http4s.{Method, Request, Status}
import shared.api.{ApiMessages, ApiPath, HttpApiError, PathParams}

import java.sql.Connection

/** 评估把题目加入比赛时是否会扩大可见范围的认证 API，只有比赛和题目管理者可调用。 */
object EvaluateContestProblemAttachWarning extends AuthenticatedApi[(ContestSlug, ProblemSlug), ContestProblemAttachWarningResponse]:

  override val method: Method = Method.GET
  override val path: ApiPath = ApiPath("/api/contests/:contestSlug/problems/:problemSlug/attach-warning")
  override val successStatus: Status = Status.Ok
  override protected val outputEncoder: Encoder[ContestProblemAttachWarningResponse] =
    summon[Encoder[ContestProblemAttachWarningResponse]]

  /** 从路径解析比赛 slug 和题目 slug，查询型入口不读取请求体。 */
  override def decode(request: Request[IO], pathParams: PathParams): IO[(ContestSlug, ProblemSlug)] =
    val _ = request
    for
      contestSlug <- HttpApiError.fromEitherBadRequest(pathParams.require("contestSlug").flatMap(ContestSlug.parse))
      problemSlug <- HttpApiError.fromEitherBadRequest(pathParams.require("problemSlug").flatMap(ProblemSlug.parse))
    yield (contestSlug, problemSlug)

  /** 校验比赛管理权和题目管理权后，检查题目是否已有比赛管理员之外的受众。 */
  override def plan(
    connection: Connection,
    actor: AuthenticatedUser,
    input: (ContestSlug, ProblemSlug)
  ): IO[ContestProblemAttachWarningResponse] =
    val (contestSlug, problemSlug) = input
    for
      maybeContest <- ContestTable.findBySlug(connection, contestSlug)
      contest <- maybeContest match
        case Some(contest) => IO.pure(contest)
        case None => HttpApiError.raise(HttpApiError.notFound(ApiMessages.contestNotFound))
      actorGroupSlugs <- ListUserGroupSlugsForMember.plan(connection, actor.username)
      _ <- HttpApiError.ensure(
        ContestAccessRules.canManageContest(actor, contest, actorGroupSlugs.slugs.toSet),
        HttpApiError.forbidden(ApiMessages.contestManagerRequired)
      )
      problemAccess <- EvaluateProblemAccess.plan(connection, actor, problemSlug)
      /** 注意：题目不存在或调用者不能管理题目都返回 404，用于避免暴露不可管理题目的存在性。 */
      problem <- problemAccess.problem match
        case Some(problem) if problemAccess.canManage => IO.pure(problem)
        case _ => HttpApiError.raise(HttpApiError.notFound(ApiMessages.problemNotFound))
      shouldWarn <- ContestProblemVisibilityTable.hasOutsideContestManagerAudience(connection, contest.id, problem.id)
    yield ContestProblemAttachWarningResponse(shouldWarn = shouldWarn)
