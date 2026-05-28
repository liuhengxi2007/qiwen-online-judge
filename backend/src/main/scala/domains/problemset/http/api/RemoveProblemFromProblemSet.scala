package domains.problemset.http.api

import cats.effect.IO
import domains.auth.http.AuthenticatedApi
import domains.auth.objects.AuthUser
import domains.problem.objects.ProblemSlug
import domains.problem.table.problem.ProblemQueryTable
import domains.problemset.http.ProblemSetApiSupport
import domains.problemset.http.codec.ProblemSetHttpCodecs.given
import domains.problemset.objects.ProblemSetSlug
import domains.problemset.objects.response.ProblemSetDetail
import domains.problemset.rules.ProblemSetAccessRules
import domains.problemset.table.problem_set.ProblemSetTable
import io.circe.Encoder
import org.http4s.{Method, Request, Status}
import shared.http.{ApiMessages, ApiPath, HttpApiError, PathParams}

import java.sql.Connection

object RemoveProblemFromProblemSet extends AuthenticatedApi[(ProblemSetSlug, ProblemSlug), ProblemSetDetail]:

  override val method: Method = Method.POST
  override val path: ApiPath = ApiPath("/api/problem-sets/:problemSetSlug/problems/:problemSlug/remove")
  override val successStatus: Status = Status.Ok
  override protected val outputEncoder: Encoder[ProblemSetDetail] = summon[Encoder[ProblemSetDetail]]

  override def decode(request: Request[IO], pathParams: PathParams): IO[(ProblemSetSlug, ProblemSlug)] =
    val _ = request
    HttpApiError.fromEitherBadRequest(
      for
        problemSetSlug <- pathParams.require("problemSetSlug").flatMap(ProblemSetSlug.parse)
        problemSlug <- pathParams.require("problemSlug").flatMap(ProblemSlug.parse)
      yield (problemSetSlug, problemSlug)
    )

  override def plan(
    connection: Connection,
    actor: AuthUser,
    input: (ProblemSetSlug, ProblemSlug)
  ): IO[ProblemSetDetail] =
    val (problemSetSlug, problemSlug) = input
    for
      _ <- HttpApiError.ensure(
        ProblemSetAccessRules.canManageProblemSets(actor),
        HttpApiError.notFound(ApiMessages.problemSetNotFound)
      )
      maybeProblemSet <- ProblemSetTable.findBySlug(connection, problemSetSlug)
      problemSet <- maybeProblemSet match
        case Some(problemSet) => IO.pure(problemSet)
        case None => HttpApiError.raise(HttpApiError.notFound(ApiMessages.problemSetNotFound))
      maybeProblem <- ProblemQueryTable.findBySlug(connection, problemSlug)
      problem <- maybeProblem match
        case Some(problem) => IO.pure(problem)
        case None => HttpApiError.raise(HttpApiError.notFound(ApiMessages.problemNotFound))
      _ <- ProblemSetTable.removeProblem(connection, problemSet.id, problem.id).flatMap {
        case ProblemSetTable.RemoveProblemTableResult.NotLinked =>
          HttpApiError.raise(HttpApiError.notFound(ApiMessages.problemNotLinkedToProblemSet))
        case ProblemSetTable.RemoveProblemTableResult.Removed =>
          IO.unit
      }
      updatedProblemSet <- ProblemSetTable.findBySlug(connection, problemSet.slug).flatMap {
        case Some(problemSet) => IO.pure(problemSet)
        case None => HttpApiError.raise(HttpApiError.internal("Problem set disappeared after problem removal."))
      }
    yield ProblemSetApiSupport.toProblemSetDetail(updatedProblemSet)
