package domains.contest.api

import cats.effect.IO
import domains.auth.api.AuthenticatedApi
import domains.auth.objects.internal.AuthenticatedUser
import domains.contest.objects.ContestSlug
import domains.problem.api.UpdateProblem
import domains.problem.objects.ProblemSlug
import domains.problem.objects.request.UpdateProblemRequest
import domains.problem.objects.response.ProblemDetail
import io.circe.Encoder
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.{Method, Request, Status}
import shared.api.{ApiPath, HttpApiError, PathParams}

import java.sql.Connection

object UpdateContestProblem extends AuthenticatedApi[(ContestSlug, ProblemSlug, UpdateProblemRequest), ProblemDetail]:

  override val method: Method = Method.POST
  override val path: ApiPath = ApiPath("/api/contests/:contestSlug/problems/:problemSlug/update")
  override val successStatus: Status = Status.Ok
  override protected val outputEncoder: Encoder[ProblemDetail] = summon[Encoder[ProblemDetail]]

  override def decode(request: Request[IO], pathParams: PathParams): IO[(ContestSlug, ProblemSlug, UpdateProblemRequest)] =
    for
      contestSlug <- HttpApiError.fromEitherBadRequest(pathParams.require("contestSlug").flatMap(ContestSlug.parse))
      problemSlug <- HttpApiError.fromEitherBadRequest(pathParams.require("problemSlug").flatMap(ProblemSlug.parse))
      body <- request.as[UpdateProblemRequest]
    yield (contestSlug, problemSlug, body)

  override def plan(
    connection: Connection,
    actor: AuthenticatedUser,
    input: (ContestSlug, ProblemSlug, UpdateProblemRequest)
  ): IO[ProblemDetail] =
    val (contestSlug, problemSlug, request) = input
    for
      validRequest <- UpdateProblem.validateRequest(request)
      problem <- ContestProblemApiSupport.requireManageLinkedProblem(connection, actor, contestSlug, problemSlug)
      updatedProblem <- UpdateProblem.updateManagedProblem(connection, problem, validRequest)
    yield updatedProblem
