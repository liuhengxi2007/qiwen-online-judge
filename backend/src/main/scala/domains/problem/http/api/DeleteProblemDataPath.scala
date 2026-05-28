package domains.problem.http.api

import cats.effect.IO
import cats.syntax.all.*
import domains.auth.http.AuthenticatedApi
import domains.auth.objects.AuthUser
import domains.problem.application.ProblemDataStorage
import domains.problem.http.ProblemDataApiSupport
import domains.problem.http.codec.ProblemHttpCodecs.given
import domains.problem.objects.ProblemSlug
import domains.problem.objects.request.DeleteProblemDataPathRequest
import domains.problem.objects.response.ProblemDetail
import domains.problem.table.problem.ProblemDataStateTable
import domains.problem.table.problem_data_file.ProblemDataFileTable
import io.circe.Encoder
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.{Method, Request, Status}
import shared.http.{ApiMessages, ApiPath, HttpApiError, PathParams}

import java.sql.Connection
import java.time.Instant

final case class DeleteProblemDataPath(problemDataStorage: ProblemDataStorage)
    extends AuthenticatedApi[(ProblemSlug, DeleteProblemDataPathRequest), ProblemDetail]:

  override val method: Method = Method.POST
  override val path: ApiPath = ApiPath("/api/problems/:problemSlug/data/file/delete")
  override val successStatus: Status = Status.Ok
  override protected val outputEncoder: Encoder[ProblemDetail] = summon[Encoder[ProblemDetail]]

  override def decode(request: Request[IO], pathParams: PathParams): IO[(ProblemSlug, DeleteProblemDataPathRequest)] =
    for
      problemSlug <- HttpApiError.fromEitherBadRequest(pathParams.require("problemSlug").flatMap(ProblemSlug.parse))
      body <- request.as[DeleteProblemDataPathRequest]
    yield (problemSlug, body)

  override def plan(
    connection: Connection,
    actor: AuthUser,
    input: (ProblemSlug, DeleteProblemDataPathRequest)
  ): IO[ProblemDetail] =
    val (problemSlug, request) = input
    ProblemDataApiSupport.withManageableProblemForUpdate(connection, actor, problemSlug) { problem =>
      for
        entries <- ProblemDataFileTable.listForProblem(connection, problem.id)
        pathsToDelete = entries.map(_.path).filter(entryPath => entryPath == request.path || entryPath.value.startsWith(s"${request.path.value}/"))
        snapshot <- problemDataStorage.snapshotDirectory(problem.slug)
        deletedProblem <-
          if pathsToDelete.isEmpty then
            HttpApiError.raise(HttpApiError.notFound(ApiMessages.problemDataFileNotFound))
          else
            pathsToDelete
              .traverse_(pathToDelete => problemDataStorage.deletePath(problem.slug, pathToDelete).void)
              .flatMap(_ => pathsToDelete.traverse_(pathToDelete => ProblemDataFileTable.deleteForProblemPath(connection, problem.id, pathToDelete)))
              .flatMap(_ => ProblemDataFileTable.listForProblem(connection, problem.id))
              .flatMap(entries =>
                ProblemDataStateTable.updateData(
                  connection,
                  problem.id,
                  Instant.now(),
                  ProblemDataApiSupport.summaryFilenameForEntries(entries)
                )
              )
              .flatMap(_ => ProblemDataApiSupport.refreshedManagedProblem(connection, problem, "Problem disappeared after data deletion."))
              .handleErrorWith { error =>
                problemDataStorage.restoreDirectory(problem.slug, snapshot) *> IO.raiseError(error)
              }
      yield deletedProblem
    }
