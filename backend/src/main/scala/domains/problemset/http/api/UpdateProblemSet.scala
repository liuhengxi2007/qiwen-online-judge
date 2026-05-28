package domains.problemset.http.api

import cats.effect.IO
import domains.auth.http.AuthenticatedApi
import domains.auth.objects.AuthUser
import domains.problemset.http.ProblemSetApiSupport
import domains.problemset.http.codec.ProblemSetHttpCodecs.given
import domains.problemset.objects.*
import domains.problemset.objects.request.UpdateProblemSetRequest
import domains.problemset.objects.response.ProblemSetDetail
import domains.problemset.rules.ProblemSetAccessRules
import domains.problemset.table.problem_set.ProblemSetTable
import io.circe.Encoder
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.{Method, Request, Status}
import shared.http.{ApiMessages, ApiPath, HttpApiError, PathParams}

import java.sql.Connection

object UpdateProblemSet extends AuthenticatedApi[(ProblemSetSlug, UpdateProblemSetRequest), ProblemSetDetail]:

  override val method: Method = Method.POST
  override val path: ApiPath = ApiPath("/api/problem-sets/:problemSetSlug")
  override val successStatus: Status = Status.Ok
  override protected val outputEncoder: Encoder[ProblemSetDetail] = summon[Encoder[ProblemSetDetail]]

  override def decode(request: Request[IO], pathParams: PathParams): IO[(ProblemSetSlug, UpdateProblemSetRequest)] =
    for
      problemSetSlug <- HttpApiError.fromEitherBadRequest(pathParams.require("problemSetSlug").flatMap(ProblemSetSlug.parse))
      body <- request.as[UpdateProblemSetRequest]
    yield (problemSetSlug, body)

  override def plan(
    connection: Connection,
    actor: AuthUser,
    input: (ProblemSetSlug, UpdateProblemSetRequest)
  ): IO[ProblemSetDetail] =
    val (problemSetSlug, request) = input
    for
      _ <- HttpApiError.ensure(
        ProblemSetAccessRules.canManageProblemSets(actor),
        HttpApiError.notFound(ApiMessages.problemSetNotFound)
      )
      title <- HttpApiError.fromEitherBadRequest(ProblemSetTitle.parse(request.title.value))
      description <- HttpApiError.fromEitherBadRequest(ProblemSetDescription.parse(request.description.value))
      validRequest = request.copy(title = title, description = description)
      maybeProblemSet <- ProblemSetTable.findBySlug(connection, problemSetSlug)
      problemSet <- maybeProblemSet match
        case Some(problemSet) => IO.pure(problemSet)
        case None => HttpApiError.raise(HttpApiError.notFound(ApiMessages.problemSetNotFound))
      _ <- ProblemSetApiSupport.validateAccessPolicySubjects(connection, validRequest.accessPolicy)
      _ <- ProblemSetTable.update(connection, problemSet.id, ProblemSetApiSupport.sanitizePolicy(validRequest))
      updatedProblemSet <- ProblemSetTable.findBySlug(connection, problemSet.slug).flatMap {
        case Some(problemSet) => IO.pure(problemSet)
        case None => HttpApiError.raise(HttpApiError.internal("Problem set disappeared after update."))
      }
    yield ProblemSetApiSupport.toProblemSetDetail(updatedProblemSet)
