package domains.problem.http.api

import cats.effect.IO
import domains.auth.http.AuthenticatedApi
import domains.auth.objects.AuthUser
import domains.problem.http.ProblemApiValidation
import domains.problem.http.codec.ProblemHttpCodecs.given
import domains.problem.objects.*
import domains.problem.objects.request.UpdateProblemRequest
import domains.problem.objects.response.ProblemDetail
import domains.problem.rules.ProblemAccessRules
import domains.problem.table.problem.{ProblemMutationTable, ProblemQueryTable}
import io.circe.Encoder
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.{Method, Request, Status}
import shared.http.{ApiMessages, ApiPath, HttpApiError, PathParams}

import java.sql.Connection
import java.time.Instant

object UpdateProblem extends AuthenticatedApi[(ProblemSlug, UpdateProblemRequest), ProblemDetail]:

  override val method: Method = Method.POST
  override val path: ApiPath = ApiPath("/api/problems/:problemSlug")
  override val successStatus: Status = Status.Ok
  override protected val outputEncoder: Encoder[ProblemDetail] = summon[Encoder[ProblemDetail]]

  override def decode(request: Request[IO], pathParams: PathParams): IO[(ProblemSlug, UpdateProblemRequest)] =
    for
      problemSlug <- HttpApiError.fromEitherBadRequest(pathParams.require("problemSlug").flatMap(ProblemSlug.parse))
      body <- request.as[UpdateProblemRequest]
    yield (problemSlug, body)

  override def plan(
    connection: Connection,
    actor: AuthUser,
    input: (ProblemSlug, UpdateProblemRequest)
  ): IO[ProblemDetail] =
    val (problemSlug, request) = input
    for
      title <- HttpApiError.fromEitherBadRequest(ProblemTitle.parse(request.title.value))
      statement <- HttpApiError.fromEitherBadRequest(ProblemStatementText.parse(request.statement.value))
      timeLimitMs <- HttpApiError.fromEitherBadRequest(ProblemTimeLimitMs.parse(request.timeLimitMs.value))
      spaceLimitMb <- HttpApiError.fromEitherBadRequest(ProblemSpaceLimitMb.parse(request.spaceLimitMb.value))
      validRequest = request.copy(
        title = title,
        statement = statement,
        timeLimitMs = timeLimitMs,
        spaceLimitMb = spaceLimitMb
      )
      maybeProblem <- ProblemQueryTable.findBySlug(connection, problemSlug)
      problem <- maybeProblem match
        case Some(problem) => IO.pure(problem)
        case None => HttpApiError.raise(HttpApiError.notFound(ApiMessages.problemNotFound))
      canManage <- ProblemAccessRules.canManageProblem(connection, actor, problem)
      _ <- HttpApiError.ensure(canManage, HttpApiError.notFound(ApiMessages.problemNotFound))
      _ <- ProblemApiValidation.validateAccessPolicySubjects(connection, validRequest.accessPolicy)
      _ <- ProblemMutationTable.update(connection, problem.id, Instant.now(), validRequest)
      updatedProblem <- ProblemQueryTable.findBySlug(connection, problem.slug).flatMap {
        case Some(problem) => IO.pure(problem.copy(canManage = true))
        case None => HttpApiError.raise(HttpApiError.internal("Problem disappeared after update."))
      }
    yield updatedProblem
