package domains.contest.api

import cats.effect.IO
import domains.auth.api.AuthenticatedApi
import domains.auth.objects.internal.AuthenticatedUser
import domains.contest.objects.ContestSlug
import domains.contest.objects.response.ContestProblemAttachWarningResponse
import domains.contest.table.contest.{ContestProblemVisibilityTable, ContestTable}
import domains.contest.utils.ContestAccessRules
import domains.problem.api.EvaluateProblemAccess
import domains.problem.objects.ProblemSlug
import domains.usergroup.api.ListUserGroupSlugsForMember
import io.circe.Encoder
import org.http4s.{Method, Request, Status}
import shared.api.{ApiMessages, ApiPath, HttpApiError, PathParams}

import java.sql.Connection

object EvaluateContestProblemAttachWarning extends AuthenticatedApi[(ContestSlug, ProblemSlug), ContestProblemAttachWarningResponse]:

  override val method: Method = Method.GET
  override val path: ApiPath = ApiPath("/api/contests/:contestSlug/problems/:problemSlug/attach-warning")
  override val successStatus: Status = Status.Ok
  override protected val outputEncoder: Encoder[ContestProblemAttachWarningResponse] =
    summon[Encoder[ContestProblemAttachWarningResponse]]

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
  ): IO[ContestProblemAttachWarningResponse] =
    val (contestSlug, problemSlug) = input
    for
      maybeContest <- ContestTable.findBySlug(connection, contestSlug)
      contest <- maybeContest match
        case Some(contest) => IO.pure(contest)
        case None => HttpApiError.raise(HttpApiError.notFound(ApiMessages.contestNotFound))
      actorGroupSlugs <- ListUserGroupSlugsForMember.plan(connection, actor.username)
      _ <- HttpApiError.ensure(
        ContestAccessRules.canManageContest(actor, contest, actorGroupSlugs.slugs.toSet),
        HttpApiError.forbidden(ApiMessages.contestManagerRequired)
      )
      problemAccess <- EvaluateProblemAccess.plan(connection, actor, problemSlug)
      problem <- problemAccess.problem match
        case Some(problem) if problemAccess.canManage => IO.pure(problem)
        case _ => HttpApiError.raise(HttpApiError.notFound(ApiMessages.problemNotFound))
      shouldWarn <- ContestProblemVisibilityTable.hasOutsideContestManagerAudience(connection, contest.id, problem.id)
    yield ContestProblemAttachWarningResponse(shouldWarn = shouldWarn)
