package domains.contest.api

import cats.effect.IO
import domains.auth.api.AuthenticatedApi
import domains.auth.objects.internal.AuthenticatedUser
import domains.contest.objects.ContestSlug
import domains.problem.api.DeleteProblem
import domains.problem.objects.ProblemSlug
import domains.submission.utils.SubmissionProgramStorage
import io.circe.Encoder
import org.http4s.{Method, Request, Status}
import shared.api.{ApiPath, HttpApiError, PathParams}
import shared.objects.response.SuccessResponse

import java.sql.Connection

final case class DeleteContestProblem(submissionProgramStorage: SubmissionProgramStorage)
    extends AuthenticatedApi[(ContestSlug, ProblemSlug), SuccessResponse]:

  override val method: Method = Method.POST
  override val path: ApiPath = ApiPath("/api/contests/:contestSlug/problems/:problemSlug/delete-problem")
  override val successStatus: Status = Status.Ok
  override protected val outputEncoder: Encoder[SuccessResponse] = summon[Encoder[SuccessResponse]]

  override def decode(request: Request[IO], pathParams: PathParams): IO[(ContestSlug, ProblemSlug)] =
    val _ = request
    for
      contestSlug <- HttpApiError.fromEitherBadRequest(pathParams.require("contestSlug").flatMap(ContestSlug.parse))
      problemSlug <- HttpApiError.fromEitherBadRequest(pathParams.require("problemSlug").flatMap(ProblemSlug.parse))
    yield (contestSlug, problemSlug)

  override def plan(
    connection: Connection,
    actor: AuthenticatedUser,
    input: (ContestSlug, ProblemSlug)
  ): IO[SuccessResponse] =
    val (contestSlug, problemSlug) = input
    for
      problem <- ContestProblemApiSupport.requireManageLinkedProblem(connection, actor, contestSlug, problemSlug)
      response <- DeleteProblem(submissionProgramStorage).deleteManagedProblem(connection, problem)
    yield response
