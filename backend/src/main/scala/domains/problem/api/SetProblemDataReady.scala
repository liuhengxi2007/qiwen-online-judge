package domains.problem.api

import cats.effect.IO
import cats.syntax.all.*
import domains.auth.api.AuthenticatedApi
import domains.auth.objects.AuthUser
import domains.judge.utils.JudgeTaskBuilder
import domains.problem.utils.ProblemDataStorage
import domains.problem.utils.ProblemDataApiHelpers

import domains.problem.objects.{ProblemDataPath, ProblemSlug}
import domains.problem.objects.internal.ProblemDataManifestEntry
import domains.problem.objects.response.ProblemDetail
import domains.problem.table.problem.ProblemDataStateTable
import domains.problem.table.problem_data_file.ProblemDataFileTable
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.deriveDecoder
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.{Method, Request, Status}
import shared.api.{ApiPath, HttpApiError, PathParams}

import java.sql.Connection
import java.time.Instant

final case class SetProblemReadyRequest(ready: Boolean)

final case class SetProblemDataReady(problemDataStorage: ProblemDataStorage)
    extends AuthenticatedApi[(ProblemSlug, SetProblemReadyRequest), ProblemDetail]:

  private given Decoder[SetProblemReadyRequest] = deriveDecoder[SetProblemReadyRequest]

  override val method: Method = Method.POST
  override val path: ApiPath = ApiPath("/api/problems/:problemSlug/data/ready-state")
  override val successStatus: Status = Status.Ok
  override protected val outputEncoder: Encoder[ProblemDetail] = summon[Encoder[ProblemDetail]]

  override def decode(request: Request[IO], pathParams: PathParams): IO[(ProblemSlug, SetProblemReadyRequest)] =
    for
      problemSlug <- HttpApiError.fromEitherBadRequest(pathParams.require("problemSlug").flatMap(ProblemSlug.parse))
      body <- request.as[SetProblemReadyRequest]
    yield (problemSlug, body)

  override def plan(
    connection: Connection,
    actor: AuthUser,
    input: (ProblemSlug, SetProblemReadyRequest)
  ): IO[ProblemDetail] =
    val (problemSlug, request) = input
    ProblemDataApiHelpers.withManageableProblemForUpdate(connection, actor, problemSlug) { problem =>
      if request.ready then markProblemReady(connection, problem)
      else markProblemNotReady(connection, problem)
    }

  private def markProblemNotReady(connection: Connection, problem: ProblemDetail): IO[ProblemDetail] =
    ProblemDataStateTable
      .updateDataReady(connection, problem.id, Instant.now(), problem.data.value, ready = false)
      .flatMap(_ => ProblemDataApiHelpers.refreshedManagedProblem(connection, problem, "Problem disappeared after ready update."))

  private def markProblemReady(
    connection: Connection,
    problem: ProblemDetail
  ): IO[ProblemDetail] =
    val judgeYamlPath = ProblemDataPath("judge.yaml")
    for
      manifest <- ProblemDataFileTable.manifestForProblem(connection, problem.id, problem.slug)
      maybeConfig <- problemDataStorage.readPath(problem.slug, judgeYamlPath)
      result <- maybeConfig match
        case None =>
          HttpApiError.raise(HttpApiError.badRequest("judge.yaml is required at the problem data root."))
        case Some((_, bytes)) =>
          JudgeTaskBuilder
            .validateReadyConfigBytes(bytes, problem, manifest)
            .fold(
              message => HttpApiError.raise(HttpApiError.badRequest(message)),
              validation => retainReadyFiles(connection, problem, manifest.entries, validation.retainedPaths)
            )
    yield result

  private def retainReadyFiles(
    connection: Connection,
    problem: ProblemDetail,
    entries: List[ProblemDataManifestEntry],
    retainedPaths: Set[ProblemDataPath]
  ): IO[ProblemDetail] =
    val retainedEntries = entries.filter(entry => retainedPaths.contains(entry.path))
    val redundantPaths = entries.map(_.path).filterNot(retainedPaths.contains)
    for
      snapshot <- problemDataStorage.snapshotDirectory(problem.slug)
      updatedProblem <- redundantPaths
        .traverse_(path => problemDataStorage.deletePath(problem.slug, path).void)
        .flatMap(_ => ProblemDataFileTable.deleteExceptPaths(connection, problem.id, retainedPaths))
        .flatMap(_ =>
          ProblemDataStateTable.updateDataReady(
            connection,
            problem.id,
            Instant.now(),
            ProblemDataApiHelpers.summaryFilenameForEntries(retainedEntries),
            ready = true
          )
        )
        .flatMap(_ => ProblemDataApiHelpers.refreshedManagedProblem(connection, problem, "Problem disappeared after ready update."))
        .handleErrorWith { error =>
          problemDataStorage.restoreDirectory(problem.slug, snapshot) *> IO.raiseError(error)
        }
    yield updatedProblem
