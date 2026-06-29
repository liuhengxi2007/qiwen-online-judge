package domains.contest.api

import cats.effect.IO
import domains.auth.api.AuthenticatedApi
import domains.auth.objects.internal.AuthenticatedUser
import domains.contest.objects.ContestSlug
import domains.problem.api.ProblemApiSupport
import domains.problem.objects.ProblemSlug
import domains.problem.objects.response.ProblemDetail
import io.circe.Encoder
import org.http4s.{Method, Request, Status}
import shared.api.{ApiMessages, ApiPath, HttpApiError, PathParams}

import java.sql.Connection
/** 获取赛内题目详情的认证 API，会把赛内权限结果映射到题目的可管理状态。 */
object GetContestProblem extends AuthenticatedApi[(ContestSlug, ProblemSlug), ProblemDetail]:

  override val method: Method = Method.GET
  override val path: ApiPath = ApiPath("/api/contests/:contestSlug/problems/:problemSlug")
  override val successStatus: Status = Status.Ok
  override protected val outputEncoder: Encoder[ProblemDetail] = summon[Encoder[ProblemDetail]]

  /** 从路径解析比赛 slug 和题目 slug，查询型入口不读取请求体。 */
  override def decode(request: Request[IO], pathParams: PathParams): IO[(ContestSlug, ProblemSlug)] =
    val _ = request
    for
      contestSlug <- HttpApiError.fromEitherBadRequest(pathParams.require("contestSlug").flatMap(ContestSlug.parse))
      problemSlug <- HttpApiError.fromEitherBadRequest(pathParams.require("problemSlug").flatMap(ProblemSlug.parse))
    yield (contestSlug, problemSlug)

  /** 先加载题目，再评估赛内访问；无权查看比赛或题目时按资源不存在返回。 */
  override def plan(
    connection: Connection,
    actor: AuthenticatedUser,
    input: (ContestSlug, ProblemSlug)
  ): IO[ProblemDetail] =
    val (contestSlug, problemSlug) = input
    for
      problem <- ProblemApiSupport.loadProblemBySlug(connection, problemSlug)
      maybeContestAccess <- EvaluateContestAccess.plan(connection, actor, EvaluateContestAccess.Input(contestSlug, Some(problem.id)))
      contestAccess <- maybeContestAccess match
        case Some(contestAccess) => IO.pure(contestAccess)
        case None => HttpApiError.raise(HttpApiError.notFound(ApiMessages.contestNotFound))
      /** 注意：无权访问比赛详情返回 404，用于隐藏受限比赛。 */
      _ <- HttpApiError.ensure(
        contestAccess.canViewContestDetail,
        HttpApiError.notFound(ApiMessages.contestNotFound)
      )
      /** 注意：题目不在比赛内或不可见时返回 404，用于隐藏赛内题目关联。 */
      _ <- HttpApiError.ensure(
        contestAccess.canViewLinkedContestProblem,
        HttpApiError.notFound(ApiMessages.problemNotFound)
      )
    yield problem.copy(canManage = contestAccess.canManageLinkedContestProblem)
