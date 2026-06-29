package domains.submission.api

import cats.effect.IO
import domains.auth.api.AuthenticatedApi
import domains.auth.objects.internal.AuthenticatedUser
import domains.problem.api.EvaluateProblemAccess
import domains.submission.objects.SubmissionId
import domains.submission.table.submission.{SubmissionMutationTable, SubmissionQueryTable}
import io.circe.Encoder
import org.http4s.{Method, Request, Status}

import shared.api.{ApiMessages, ApiPath, HttpApiError, PathParams}
import shared.objects.transport.SuccessResponse

import java.sql.Connection

/** 删除提交的管理端 API；只有目标题目的管理者可删除，并尽力清理源码对象。 */
final case class DeleteSubmission(submissionProgramStorage: SubmissionProgramStorageContext) extends AuthenticatedApi[SubmissionId, SuccessResponse]:

  override val method: Method = Method.POST
  override val path: ApiPath = ApiPath("/api/submissions/:submissionId/delete")
  override val successStatus: Status = Status.Ok
  override protected val outputEncoder: Encoder[SuccessResponse] = summon[Encoder[SuccessResponse]]

  /** 从路径解析提交 public id；请求体无业务含义。 */
  override def decode(request: Request[IO], pathParams: PathParams): IO[SubmissionId] =
    val _ = request
    HttpApiError.fromEitherBadRequest(pathParams.require("submissionId").flatMap(SubmissionId.parse))

  /** 校验题目管理权限后删除提交；无权或题目不可见时统一隐藏提交存在性。 */
  override def plan(connection: Connection, actor: AuthenticatedUser, submissionId: SubmissionId): IO[SuccessResponse] =
    for
      maybeRecord <- SubmissionQueryTable.findById(connection, submissionId)
      record <- maybeRecord match
        case Some(record) => IO.pure(record)
        case None => HttpApiError.raise(HttpApiError.notFound(ApiMessages.submissionNotFound))
      access <- EvaluateProblemAccess.plan(connection, actor, record.problemSlug)
      _ <- access.problem match
        case Some(problem) => IO.pure(problem)
        case None => HttpApiError.raise(HttpApiError.notFound(ApiMessages.submissionNotFound))
      _ <- HttpApiError.ensure(access.canManage, HttpApiError.notFound(ApiMessages.submissionNotFound))
      _ <- SubmissionMutationTable.deleteById(connection, submissionId)
      // 注意：对象存储删除是 best-effort，数据库删除成功后不会因源码对象清理失败回滚。
      _ <- SubmissionProgramStorage.deleteManifest(submissionProgramStorage, record.programManifest).handleError(_ => ())
    yield SuccessResponse(code = Some(ApiMessages.submissionDeleted.code), message = None, params = ApiMessages.submissionDeleted.params)
