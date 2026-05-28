package domains.submission.http.api

import cats.effect.IO
import domains.auth.http.AuthenticatedApi
import domains.auth.objects.AuthUser
import domains.problem.rules.ProblemAccessRules
import domains.problem.table.problem.ProblemQueryTable
import domains.submission.objects.SubmissionId
import domains.submission.table.submission.{SubmissionMutationTable, SubmissionQueryTable}
import io.circe.Encoder
import org.http4s.{Method, Request, Status}
import shared.http.codec.SharedHttpCodecs.given
import shared.http.{ApiMessages, ApiPath, HttpApiError, PathParams}
import shared.objects.response.SuccessResponse

import java.sql.Connection

object DeleteSubmission extends AuthenticatedApi[SubmissionId, SuccessResponse]:

  override val method: Method = Method.POST
  override val path: ApiPath = ApiPath("/api/submissions/:submissionId/delete")
  override val successStatus: Status = Status.Ok
  override protected val outputEncoder: Encoder[SuccessResponse] = summon[Encoder[SuccessResponse]]

  override def decode(request: Request[IO], pathParams: PathParams): IO[SubmissionId] =
    val _ = request
    HttpApiError.fromEitherBadRequest(pathParams.require("submissionId").flatMap(SubmissionId.parse))

  override def plan(connection: Connection, actor: AuthUser, submissionId: SubmissionId): IO[SuccessResponse] =
    for
      maybeSubmission <- SubmissionQueryTable.findById(connection, submissionId)
      submission <- maybeSubmission match
        case Some(submission) => IO.pure(submission)
        case None => HttpApiError.raise(HttpApiError.notFound(ApiMessages.submissionNotFound))
      maybeProblem <- ProblemQueryTable.findBySlug(connection, submission.problemSlug)
      problem <- maybeProblem match
        case Some(problem) => IO.pure(problem)
        case None => HttpApiError.raise(HttpApiError.notFound(ApiMessages.submissionNotFound))
      access <- ProblemAccessRules.evaluateProblemPermissions(connection, actor, problem)
      _ <- HttpApiError.ensure(access.canManage, HttpApiError.notFound(ApiMessages.submissionNotFound))
      _ <- SubmissionMutationTable.deleteById(connection, submissionId)
    yield SuccessResponse(code = Some(ApiMessages.submissionDeleted.code), message = None, params = ApiMessages.submissionDeleted.params)
