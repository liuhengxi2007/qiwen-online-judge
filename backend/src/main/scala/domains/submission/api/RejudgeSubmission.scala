package domains.submission.api

import cats.effect.IO
import domains.auth.api.AuthenticatedApi
import domains.auth.objects.internal.AuthenticatedUser
import domains.problem.api.EvaluateProblemAccess

import domains.submission.objects.{SubmissionId, SubmissionStatus}
import domains.submission.objects.response.SubmissionDetail
import domains.submission.table.submission.{SubmissionJudgeTable, SubmissionQueryTable}
import domains.submission.utils.SubmissionProgramStorage
import io.circe.Encoder
import org.http4s.{Method, Request, Status}
import shared.api.{ApiMessages, ApiPath, HttpApiError, PathParams}

import java.sql.Connection

/** 手动重判提交的管理端 API；仅题目管理者可对已完成或失败的提交重新入队。 */
final case class RejudgeSubmission(submissionProgramStorage: SubmissionProgramStorage) extends AuthenticatedApi[SubmissionId, SubmissionDetail]:

  override val method: Method = Method.POST
  override val path: ApiPath = ApiPath("/api/submissions/:submissionId/rejudge")
  override val successStatus: Status = Status.Ok
  override protected val outputEncoder: Encoder[SubmissionDetail] = summon[Encoder[SubmissionDetail]]

  /** 从路径解析提交 public id。 */
  override def decode(request: Request[IO], pathParams: PathParams): IO[SubmissionId] =
    val _ = request
    HttpApiError.fromEitherBadRequest(pathParams.require("submissionId").flatMap(SubmissionId.parse))

  /** 校验管理权限和提交终态后重置判题状态；输出包含源码的更新后提交详情。 */
  override def plan(connection: Connection, actor: AuthenticatedUser, submissionId: SubmissionId): IO[SubmissionDetail] =
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
      _ <- HttpApiError.ensure(
        record.status != SubmissionStatus.Queued && record.status != SubmissionStatus.Running,
        HttpApiError.badRequest("Only completed or failed submissions can be rejudged.")
      )
      queued <- SubmissionJudgeTable.queueManualRejudge(connection, submissionId)
      _ <- HttpApiError.ensure(queued, HttpApiError.badRequest("Submission can no longer be rejudged."))
      updatedRecord <- SubmissionQueryTable.findById(connection, submissionId).map(
        _.getOrElse(throw new IllegalStateException("Submission disappeared after rejudge."))
      )
      sourceCodes <- submissionProgramStorage.readSources(updatedRecord.programManifest).flatMap {
        case Right(sourceCodes) => IO.pure(sourceCodes)
        case Left(message) => HttpApiError.raise(HttpApiError.internal(message))
      }
    yield SubmissionDetail.fromRecord(updatedRecord, sourceCodes, canManage = true)
