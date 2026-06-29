package domains.problem.api

import cats.effect.IO
import domains.auth.api.AuthenticatedApi
import domains.auth.objects.internal.AuthenticatedUser
import domains.submission.api.QueueManualProblemRejudgeForProblem
import io.circe.Encoder
import org.http4s.{Method, Request, Status}
import shared.api.{ApiPath, HttpApiError, PathParams}
import shared.objects.transport.SuccessResponse

import java.sql.Connection

/** 手动整题重判 API；仅题目管理者可将终态提交按中等优先级重新入队。 */
object RejudgeProblemSubmissions extends AuthenticatedApi[ProblemManagementContext, SuccessResponse]:

  override val method: Method = Method.POST
  override val path: ApiPath = ApiPath("/api/problems/:problemSlug/submissions/rejudge")
  override val successStatus: Status = Status.Ok
  override protected val outputEncoder: Encoder[SuccessResponse] = summon[Encoder[SuccessResponse]]

  /** 解析题目管理上下文；请求体无业务内容。 */
  override def decode(request: Request[IO], pathParams: PathParams): IO[ProblemManagementContext] =
    ProblemManagementContext.decode(request, pathParams)

  /** 校验题目管理权限和 ready 状态后，把该题终态提交排入手动整题重判队列。 */
  override def plan(connection: Connection, actor: AuthenticatedUser, context: ProblemManagementContext): IO[SuccessResponse] =
    for
      problem <- ProblemManagementContext.requireManagedProblem(connection, actor, context)
      _ <- HttpApiError.ensure(problem.ready, HttpApiError.badRequest("Problem data must be ready before rejudging all submissions."))
      response <- QueueManualProblemRejudgeForProblem.plan(connection, problem.id)
    yield response
