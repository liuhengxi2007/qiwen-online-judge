package domains.problem.api

import cats.effect.IO
import domains.auth.api.AuthenticatedApi
import domains.auth.objects.internal.AuthenticatedUser
import domains.problem.table.problem.ProblemMutationTable
import domains.problem.table.problem_access_grant.ProblemAccessGrantTable
import domains.submission.utils.SubmissionProgramCleanup
import domains.submission.utils.SubmissionProgramStorageContext
import io.circe.Encoder
import org.http4s.{Method, Request, Status}

import shared.api.{ApiMessages, ApiPath, PathParams}
import shared.objects.response.SuccessResponse

import java.sql.Connection

/** 删除题目的管理端 API；会先校验题目或竞赛管理权限，再删除题目记录并尽力清理关联提交程序对象。 */
final case class DeleteProblem(submissionProgramStorage: SubmissionProgramStorageContext) extends AuthenticatedApi[ProblemManagementContext, SuccessResponse]:

  override val method: Method = Method.POST
  override val path: ApiPath = ApiPath("/api/problems/:problemSlug/delete")
  override val successStatus: Status = Status.Ok
  override protected val outputEncoder: Encoder[SuccessResponse] = summon[Encoder[SuccessResponse]]

  /** 解析被删除题目的 slug 和可选竞赛上下文。 */
  override def decode(request: Request[IO], pathParams: PathParams): IO[ProblemManagementContext] =
    ProblemManagementContext.decode(request, pathParams)

  /** 校验管理权限后执行删除；对无权限直接题目访问返回隐藏存在性的错误。 */
  override def plan(
    connection: Connection,
    actor: AuthenticatedUser,
    context: ProblemManagementContext
  ): IO[SuccessResponse] =
    ProblemManagementContext
      .requireManagedProblem(connection, actor, context)
      .flatMap(problem => deleteManagedProblem(connection, problem))

  /** 删除已确认可管理的题目；副作用包括清授权、删题目行和 best-effort 删除提交程序源码对象。 */
  def deleteManagedProblem(connection: Connection, problem: domains.problem.objects.response.ProblemDetail): IO[SuccessResponse] =
    for
      deleteSubmissionPrograms <- SubmissionProgramCleanup.prepareDeleteForProblem(connection, problem.id, submissionProgramStorage)
      _ <- ProblemAccessGrantTable.deleteAllForProblem(connection, problem.id)
      _ <- ProblemMutationTable.delete(connection, problem.id)
      _ <- deleteSubmissionPrograms
    yield SuccessResponse(code = Some(ApiMessages.problemDeleted.code), message = None, params = ApiMessages.problemDeleted.params)
