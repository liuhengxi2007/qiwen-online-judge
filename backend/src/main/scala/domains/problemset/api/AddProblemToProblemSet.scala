package domains.problemset.api

import cats.effect.IO
import domains.auth.api.AuthenticatedApi
import domains.auth.objects.AuthUser
import domains.problem.objects.ProblemSlug
import domains.problem.table.problem.ProblemQueryTable

import domains.problemset.objects.ProblemSetSlug
import domains.problemset.objects.request.AddProblemToProblemSetRequest
import domains.problemset.objects.response.ProblemSetDetail
import domains.problemset.rules.ProblemSetAccessRules
import domains.problemset.table.problem_set.ProblemSetTable
import io.circe.Encoder
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.{Method, Request, Status}
import shared.api.{ApiMessages, ApiPath, HttpApiError, PathParams}

import java.sql.Connection

object AddProblemToProblemSet extends AuthenticatedApi[(ProblemSetSlug, AddProblemToProblemSetRequest), ProblemSetDetail]:

  override val method: Method = Method.POST
  override val path: ApiPath = ApiPath("/api/problem-sets/:problemSetSlug/problems")
  override val successStatus: Status = Status.Ok
  override protected val outputEncoder: Encoder[ProblemSetDetail] = summon[Encoder[ProblemSetDetail]]

  override def decode(request: Request[IO], pathParams: PathParams): IO[(ProblemSetSlug, AddProblemToProblemSetRequest)] =
    for
      problemSetSlug <- HttpApiError.fromEitherBadRequest(pathParams.require("problemSetSlug").flatMap(ProblemSetSlug.parse))
      body <- request.as[AddProblemToProblemSetRequest]
    yield (problemSetSlug, body)

  override def plan(
    connection: Connection,
    actor: AuthUser,
    input: (ProblemSetSlug, AddProblemToProblemSetRequest)
  ): IO[ProblemSetDetail] =
    val (problemSetSlug, request) = input
    for
      _ <- HttpApiError.ensure(
        ProblemSetAccessRules.canManageProblemSets(actor),
        HttpApiError.notFound(ApiMessages.problemSetNotFound)
      )
      validProblemSlug <- HttpApiError.fromEitherBadRequest(ProblemSlug.parse(request.problemSlug.value))
      maybeProblemSet <- ProblemSetTable.findBySlug(connection, problemSetSlug)
      problemSet <- maybeProblemSet match
        case Some(problemSet) => IO.pure(problemSet)
        case None => HttpApiError.raise(HttpApiError.notFound(ApiMessages.problemSetNotFound))
      maybeProblem <- ProblemQueryTable.findBySlug(connection, validProblemSlug)
      problem <- maybeProblem match
        case Some(problem) => IO.pure(problem)
        case None => HttpApiError.raise(HttpApiError.notFound(ApiMessages.problemNotFound))
      _ <- ProblemSetTable.addProblem(connection, problemSet.id, problem.id).flatMap {
        case ProblemSetTable.AddProblemTableResult.AlreadyLinked =>
          HttpApiError.raise(HttpApiError.conflict(ApiMessages.problemAlreadyLinkedToProblemSet))
        case ProblemSetTable.AddProblemTableResult.Linked =>
          IO.unit
      }
      updatedProblemSet <- ProblemSetTable.findBySlug(connection, problemSet.slug).flatMap {
        case Some(problemSet) => IO.pure(problemSet)
        case None => HttpApiError.raise(HttpApiError.internal("Problem set disappeared after problem link."))
      }
    yield ProblemSetDetail.fromProblemSet(updatedProblemSet)
