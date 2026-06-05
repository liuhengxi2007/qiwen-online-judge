package domains.problem.api

import cats.effect.IO
import domains.auth.api.AuthenticatedApi
import domains.auth.objects.internal.AuthenticatedUser
import domains.problem.utils.ProblemAccessPolicyValidation

import domains.problem.objects.*
import domains.problem.objects.request.CreateProblemRequest
import domains.problem.objects.response.ProblemDetail
import domains.problem.table.problem.{ProblemMutationTable, ProblemQueryTable}
import io.circe.Encoder
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.{Method, Request, Status}
import shared.api.{ApiMessages, ApiPath, HttpApiError, PathParams}

import java.sql.Connection
import java.util.UUID

object CreateProblem extends AuthenticatedApi[CreateProblemRequest, ProblemDetail]:

  override val method: Method = Method.POST
  override val path: ApiPath = ApiPath("/api/problems")
  override val successStatus: Status = Status.Created
  override protected val outputEncoder: Encoder[ProblemDetail] = summon[Encoder[ProblemDetail]]

  override def decode(request: Request[IO], pathParams: PathParams): IO[CreateProblemRequest] =
    val _ = pathParams
    request.as[CreateProblemRequest]

  override def plan(
    connection: Connection,
    actor: AuthenticatedUser,
    request: CreateProblemRequest
  ): IO[ProblemDetail] =
    for
      _ <- HttpApiError.ensure(
        actor.problemManager,
        HttpApiError.forbidden(ApiMessages.problemManagerRequired)
      )
      slug <- HttpApiError.fromEitherBadRequest(ProblemSlug.parse(request.slug.value))
      title <- HttpApiError.fromEitherBadRequest(ProblemTitle.parse(request.title.value))
      statement <- HttpApiError.fromEitherBadRequest(ProblemStatementText.parse(request.statement.value))
      validRequest = request.copy(
        slug = slug,
        title = title,
        statement = statement
      )
      existing <- ProblemQueryTable.findBySlug(connection, validRequest.slug)
      _ <- HttpApiError.ensure(existing.isEmpty, HttpApiError.conflict(ApiMessages.problemSlugExists))
      conflictingProblemSet <- ProblemAccessPolicyValidation.problemSetSlugExists(connection, validRequest.slug.value)
      _ <- HttpApiError.ensure(
        !conflictingProblemSet,
        HttpApiError.conflict(ApiMessages.problemSlugConflictsWithProblemSet)
      )
      _ <- ProblemAccessPolicyValidation.validateAccessPolicySubjects(connection, validRequest.accessPolicy)
      problemId <- IO.delay(ProblemId(UUID.randomUUID()))
      now <- IO.realTimeInstant
      problem <- ProblemMutationTable.insert(connection, problemId, now, actor.username, validRequest)
    yield problem.copy(canManage = true)
