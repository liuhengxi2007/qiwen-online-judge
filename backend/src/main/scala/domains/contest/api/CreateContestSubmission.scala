package domains.contest.api

import cats.effect.IO
import domains.auth.api.AuthenticatedApi
import domains.auth.objects.internal.AuthenticatedUser
import domains.contest.objects.ContestSlug
import domains.contest.table.contest.ContestTable
import domains.contest.utils.ContestAccessRules
import domains.problem.objects.ProblemSlug
import domains.problem.table.problem.ProblemQueryTable
import domains.submission.api.CreateSubmission
import domains.submission.objects.request.CreateSubmissionRequest
import domains.submission.objects.response.{SubmissionDetail, SubmissionSource}
import domains.submission.utils.SubmissionProgramStorage
import domains.usergroup.api.ListUserGroupSlugsForMember
import io.circe.Encoder
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.{Method, Request, Status}
import shared.api.{ApiMessages, ApiPath, HttpApiError, PathParams}

import java.sql.Connection
import java.time.Instant

final case class CreateContestSubmission(submissionProgramStorage: SubmissionProgramStorage)
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
    val now = Instant.now()
    for
      problemSlug <- HttpApiError.fromEitherBadRequest(ProblemSlug.parse(request.problemSlug.value))
      validRequest = request.copy(problemSlug = problemSlug)
      maybeContest <- ContestTable.findBySlug(connection, contestSlug)
      contest <- maybeContest match
        case Some(contest) => IO.pure(contest)
        case None => HttpApiError.raise(HttpApiError.notFound(ApiMessages.contestNotFound))
      actorGroupSlugs <- ListUserGroupSlugsForMember.plan(connection, actor.username)
      _ <- HttpApiError.ensure(
        ContestAccessRules.canViewContest(actor, contest, actorGroupSlugs.slugs.toSet),
        HttpApiError.notFound(ApiMessages.contestNotFound)
      )
      _ <- HttpApiError.ensure(!now.isBefore(contest.startAt), HttpApiError.badRequest(ApiMessages.contestNotRunning))
      submission <-
        if now.isAfter(contest.endAt) then
          CreateSubmission(submissionProgramStorage).createForProblem(connection, actor, validRequest)
        else
          for
            registration <- ContestTable.findRegistration(connection, contest.id, actor.username)
            _ <- HttpApiError.ensure(
              registration.exists(registeredAt => registeredAt.isBefore(contest.startAt) || registeredAt.equals(contest.startAt)),
              HttpApiError.forbidden(ApiMessages.contestNotRegistered)
            )
            problem <- ProblemQueryTable.findBySlug(connection, validRequest.problemSlug).flatMap {
              case Some(problem) => IO.pure(problem)
              case None => HttpApiError.raise(HttpApiError.notFound(ApiMessages.contestProblemNotFound))
            }
            _ <- HttpApiError.ensure(
              contest.problems.exists(contestProblem => contestProblem.id.value == problem.id.value),
              HttpApiError.notFound(ApiMessages.contestProblemNotFound)
            )
            submission <- CreateSubmission(submissionProgramStorage).createForAccessibleProblem(
              connection = connection,
              actor = actor,
              request = validRequest,
              problemId = problem.id,
              contestId = Some(contest.id),
              problemSlug = problem.slug,
              problemTitle = problem.title,
              source = SubmissionSource.fromContest(contest.slug, contest.title),
              canManage = false
            )
          yield submission
    yield submission
