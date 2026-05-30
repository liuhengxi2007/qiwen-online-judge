package domains.submission.api

import cats.effect.IO
import domains.auth.api.AuthenticatedApi
import domains.auth.objects.internal.AuthenticatedUser
import domains.problem.api.EvaluateProblemAccess

import domains.submission.objects.{SubmissionId, SubmissionStatus}
import domains.submission.objects.internal.SubmissionJudgeState
import domains.submission.objects.response.SubmissionDetail
import domains.submission.table.submission.{SubmissionJudgeTable, SubmissionQueryTable}
import io.circe.Encoder
import org.http4s.{Method, Request, Status}
import shared.api.{ApiMessages, ApiPath, HttpApiError, PathParams}

import java.sql.Connection

object RejudgeSubmission extends AuthenticatedApi[SubmissionId, SubmissionDetail]:

  override val method: Method = Method.POST
  override val path: ApiPath = ApiPath("/api/submissions/:submissionId/rejudge")
  override val successStatus: Status = Status.Ok
  override protected val outputEncoder: Encoder[SubmissionDetail] = summon[Encoder[SubmissionDetail]]

  override def decode(request: Request[IO], pathParams: PathParams): IO[SubmissionId] =
    val _ = request
    HttpApiError.fromEitherBadRequest(pathParams.require("submissionId").flatMap(SubmissionId.parse))

  override def plan(connection: Connection, actor: AuthenticatedUser, submissionId: SubmissionId): IO[SubmissionDetail] =
    for
      maybeSubmission <- SubmissionQueryTable.findById(connection, submissionId)
      submission <- maybeSubmission match
        case Some(submission) => IO.pure(submission)
        case None => HttpApiError.raise(HttpApiError.notFound(ApiMessages.submissionNotFound))
      access <- EvaluateProblemAccess.plan(connection, actor, submission.problemSlug)
      _ <- access.problem match
        case Some(problem) => IO.pure(problem)
        case None => HttpApiError.raise(HttpApiError.notFound(ApiMessages.submissionNotFound))
      _ <- HttpApiError.ensure(access.canManage, HttpApiError.notFound(ApiMessages.submissionNotFound))
      _ <- HttpApiError.ensure(
        submission.status != SubmissionStatus.Queued && submission.status != SubmissionStatus.Running,
        HttpApiError.badRequest("Only completed or failed submissions can be rejudged.")
      )
      _ <- SubmissionJudgeTable.updateJudgeState(connection, submissionId, SubmissionJudgeState.queued)
      updated <- SubmissionQueryTable.findById(connection, submissionId).map(
        _.getOrElse(throw new IllegalStateException("Submission disappeared after rejudge."))
      )
    yield updated.copy(canManage = true)
