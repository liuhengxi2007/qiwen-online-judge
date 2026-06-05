package domains.problem.api

import cats.effect.IO
import domains.auth.api.AuthenticatedApi
import domains.auth.objects.internal.AuthenticatedUser
import domains.problem.objects.ProblemSlug
import domains.problem.table.problem.ProblemMutationTable
import domains.problem.table.problem_access_grant.ProblemAccessGrantTable
import domains.submission.utils.SubmissionProgramCleanup
import domains.submission.utils.SubmissionProgramStorage
import io.circe.Encoder
import org.http4s.{Method, Request, Status}

import shared.api.{ApiMessages, ApiPath, HttpApiError, PathParams}
import shared.objects.response.SuccessResponse

import java.sql.Connection

final case class DeleteProblem(submissionProgramStorage: SubmissionProgramStorage) extends AuthenticatedApi[ProblemSlug, SuccessResponse]:

  override val method: Method = Method.POST
  override val path: ApiPath = ApiPath("/api/problems/:problemSlug/delete")
  override val successStatus: Status = Status.Ok
  override protected val outputEncoder: Encoder[SuccessResponse] = summon[Encoder[SuccessResponse]]

  override def decode(request: Request[IO], pathParams: PathParams): IO[ProblemSlug] =
    val _ = request
    HttpApiError.fromEitherBadRequest(pathParams.require("problemSlug").flatMap(ProblemSlug.parse))

  override def plan(
    connection: Connection,
    actor: AuthenticatedUser,
    problemSlug: ProblemSlug
  ): IO[SuccessResponse] =
    EvaluateProblemAccess.plan(connection, actor, problemSlug).flatMap { access =>
      access.problem match
        case None =>
          HttpApiError.raise(HttpApiError.notFound(ApiMessages.problemNotFound))
        case Some(problem) =>
          for
            _ <- HttpApiError.ensure(access.canManage, HttpApiError.notFound(ApiMessages.problemNotFound))
            response <- deleteManagedProblem(connection, problem)
          yield response
    }

  def deleteManagedProblem(connection: Connection, problem: domains.problem.objects.response.ProblemDetail): IO[SuccessResponse] =
    for
      deleteSubmissionPrograms <- SubmissionProgramCleanup.prepareDeleteForProblem(connection, problem.id, submissionProgramStorage)
      _ <- ProblemAccessGrantTable.deleteAllForProblem(connection, problem.id)
      _ <- ProblemMutationTable.delete(connection, problem.id)
      _ <- deleteSubmissionPrograms
    yield SuccessResponse(code = Some(ApiMessages.problemDeleted.code), message = None, params = ApiMessages.problemDeleted.params)
