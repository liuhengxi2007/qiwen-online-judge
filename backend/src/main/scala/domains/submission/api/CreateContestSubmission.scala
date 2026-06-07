package domains.submission.api

import cats.effect.IO
import domains.auth.api.AuthenticatedApi
import domains.auth.objects.internal.AuthenticatedUser
import domains.contest.api.EvaluateContestAccess
import domains.contest.objects.ContestSlug
import domains.problem.api.EvaluateProblemAccess
import domains.problem.objects.ProblemSlug
import domains.problem.utils.ProblemDataStorage
import domains.submission.objects.SubmissionSource
import domains.submission.objects.request.CreateSubmissionRequest
import domains.submission.objects.response.SubmissionDetail
import domains.submission.utils.SubmissionProgramStorage
import io.circe.Encoder
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.{Method, Request, Status}
import shared.api.{ApiMessages, ApiPath, HttpApiError, PathParams}

import java.sql.Connection
final case class CreateContestSubmission(
  submissionProgramStorage: SubmissionProgramStorage,
  problemDataStorage: ProblemDataStorage
)
  extends AuthenticatedApi[(ContestSlug, CreateSubmissionRequest), SubmissionDetail]:

  override val method: Method = Method.POST
  override val path: ApiPath = ApiPath("/api/contests/:contestSlug/submissions")
  override val successStatus: Status = Status.Created
  override protected val outputEncoder: Encoder[SubmissionDetail] = summon[Encoder[SubmissionDetail]]

  override def decode(request: Request[IO], pathParams: PathParams): IO[(ContestSlug, CreateSubmissionRequest)] =
    for
      contestSlug <- HttpApiError.fromEitherBadRequest(pathParams.require("contestSlug").flatMap(ContestSlug.parse))
      body <- request.as[CreateSubmissionRequest]
    yield (contestSlug, body)

  override def plan(
    connection: Connection,
    actor: AuthenticatedUser,
    input: (ContestSlug, CreateSubmissionRequest)
  ): IO[SubmissionDetail] =
    val (contestSlug, request) = input
    for
      problemSlug <- HttpApiError.fromEitherBadRequest(ProblemSlug.parse(request.problemSlug.value))
      validRequest = request.copy(problemSlug = problemSlug)
      problemAccess <- EvaluateProblemAccess.plan(connection, actor, validRequest.problemSlug)
      problem <- problemAccess.problem match
        case Some(problem) => IO.pure(problem)
        case None => HttpApiError.raise(HttpApiError.notFound(ApiMessages.contestProblemNotFound))
      maybeContestAccess <- EvaluateContestAccess.plan(connection, actor, EvaluateContestAccess.Input(contestSlug, Some(problem.id)))
      contestAccess <- maybeContestAccess match
        case Some(contestAccess) => IO.pure(contestAccess)
        case None => HttpApiError.raise(HttpApiError.notFound(ApiMessages.contestNotFound))
      _ <- HttpApiError.ensure(
        contestAccess.canViewContest,
        HttpApiError.notFound(ApiMessages.contestNotFound)
      )
      _ <- HttpApiError.ensure(
        contestAccess.contestStarted && !contestAccess.contestEnded,
        HttpApiError.badRequest(ApiMessages.contestNotRunning)
      )
      _ <- HttpApiError.ensure(
        contestAccess.containsProblem,
        HttpApiError.notFound(ApiMessages.contestProblemNotFound)
      )
      _ <- HttpApiError.ensure(
        contestAccess.canSubmitContestProblem,
        HttpApiError.forbidden(ApiMessages.contestNotRegistered)
      )
      submission <- CreateSubmission(submissionProgramStorage, problemDataStorage).createForAccessibleProblem(
        connection = connection,
        actor = actor,
        request = validRequest,
        problemId = problem.id,
        contestId = Some(contestAccess.contestId),
        problemSlug = problem.slug,
        problemTitle = problem.title,
        source = SubmissionSource.fromContest(contestAccess.contestSlug, contestAccess.contestTitle),
        canManage = false
      )
    yield submission
