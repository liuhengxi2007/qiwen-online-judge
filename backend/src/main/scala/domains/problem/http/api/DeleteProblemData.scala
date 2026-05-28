package domains.problem.http.api

import cats.effect.IO
import domains.auth.http.AuthenticatedApi
import domains.auth.objects.AuthUser
import domains.problem.utils.ProblemDataStorage
import domains.problem.http.ProblemDataApiSupport
import domains.problem.http.codec.ProblemHttpCodecs.given
import domains.problem.objects.{ProblemDataFilename, ProblemDataPath, ProblemSlug}
import domains.problem.objects.response.ProblemDetail
import domains.problem.table.problem.ProblemDataStateTable
import domains.problem.table.problem_data_file.ProblemDataFileTable
import io.circe.Encoder
import org.http4s.{Method, Request, Status}
import shared.http.{ApiMessages, ApiPath, HttpApiError, PathParams}

import java.sql.Connection
import java.time.Instant

final case class DeleteProblemData(problemDataStorage: ProblemDataStorage)
    extends AuthenticatedApi[(ProblemSlug, ProblemDataFilename), ProblemDetail]:

  override val method: Method = Method.POST
  override val path: ApiPath = ApiPath("/api/problems/:problemSlug/data/:filename/delete")
  override val successStatus: Status = Status.Ok
  override protected val outputEncoder: Encoder[ProblemDetail] = summon[Encoder[ProblemDetail]]

  override def decode(request: Request[IO], pathParams: PathParams): IO[(ProblemSlug, ProblemDataFilename)] =
    val _ = request
    HttpApiError.fromEitherBadRequest(
      for
        problemSlug <- pathParams.require("problemSlug").flatMap(ProblemSlug.parse)
        filename <- pathParams.require("filename").flatMap(ProblemDataFilename.parse)
      yield (problemSlug, filename)
    )

  override def plan(
    connection: Connection,
    actor: AuthUser,
    input: (ProblemSlug, ProblemDataFilename)
  ): IO[ProblemDetail] =
    val (problemSlug, filename) = input
    ProblemDataApiSupport.withManageableProblemForUpdate(connection, actor, problemSlug) { problem =>
      for
        snapshot <- problemDataStorage.snapshotDirectory(problem.slug)
        deletedProblem <- problemDataStorage.deleteFile(problem.slug, filename).flatMap {
          case false =>
            HttpApiError.raise(HttpApiError.notFound(ApiMessages.problemDataFileNotFound))
          case true =>
            ProblemDataFileTable
              .deleteForProblemPath(connection, problem.id, ProblemDataPath.fromFilename(filename))
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
        }
      yield deletedProblem
    }
