package domains.problem.api

import cats.effect.IO
import domains.auth.api.AuthenticatedApi
import domains.auth.objects.internal.AuthenticatedUser
import domains.auth.table.auth_account.AuthAccountTable
import domains.problem.utils.ProblemAccessPolicyValidation

import domains.problem.objects.*
import domains.problem.objects.request.UpdateProblemRequest
import domains.problem.objects.response.ProblemDetail
import domains.problem.table.problem.{ProblemMutationTable, ProblemQueryTable}
import io.circe.Encoder
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.{Method, Request, Status}
import shared.api.{ApiMessages, ApiPath, HttpApiError, PathParams}

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
    actor: AuthenticatedUser,
    input: (ProblemSlug, UpdateProblemRequest)
  ): IO[ProblemDetail] =
    val (problemSlug, request) = input
    for
      title <- HttpApiError.fromEitherBadRequest(ProblemTitle.parse(request.title.value))
      statement <- HttpApiError.fromEitherBadRequest(ProblemStatementText.parse(request.statement.value))
      validRequest = request.copy(
        title = title,
        statement = statement
      )
      access <- EvaluateProblemAccess.plan(connection, actor, problemSlug)
      problem <- access.problem match
        case Some(problem) => IO.pure(problem)
        case None => HttpApiError.raise(HttpApiError.notFound(ApiMessages.problemNotFound))
      _ <- HttpApiError.ensure(access.canManage, HttpApiError.notFound(ApiMessages.problemNotFound))
      _ <- validateAuthorUsername(connection, validRequest.authorUsername)
      _ <- ProblemAccessPolicyValidation.validateAccessPolicySubjects(connection, validRequest.accessPolicy)
      _ <- ProblemMutationTable.update(connection, problem.id, Instant.now(), validRequest)
      updatedProblem <- ProblemQueryTable.findBySlug(connection, problem.slug).flatMap {
        case Some(problem) => IO.pure(problem.copy(canManage = true))
        case None => HttpApiError.raise(HttpApiError.internal("Problem disappeared after update."))
      }
    yield updatedProblem

  private def validateAuthorUsername(connection: Connection, authorUsername: Option[domains.user.objects.Username]): IO[Unit] =
    authorUsername match
      case Some(username) =>
        AuthAccountTable.findAccountByUsername(connection, username).flatMap { account =>
          HttpApiError.ensure(account.nonEmpty, HttpApiError.badRequest(ApiMessages.userNotFound))
        }
      case None =>
        IO.unit
