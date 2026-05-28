package domains.problem.http.api

import cats.effect.IO
import domains.auth.http.AuthenticatedResponseApi
import domains.auth.objects.AuthUser
import domains.problem.utils.ProblemDataStorage
import domains.problem.objects.{ProblemDataFilename, ProblemSlug}
import domains.problem.rules.ProblemAccessRules
import domains.problem.table.problem.ProblemQueryTable
import fs2.Stream
import org.http4s.{Header, Method, Request, Response, Status}
import org.typelevel.ci.CIString
import shared.http.{ApiMessages, ApiPath, HttpApiError, PathParams}

import java.sql.Connection

final case class DownloadProblemData(problemDataStorage: ProblemDataStorage)
    extends AuthenticatedResponseApi[(ProblemSlug, ProblemDataFilename)]:

  override val method: Method = Method.GET
  override val path: ApiPath = ApiPath("/api/problems/:problemSlug/data/:filename")

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
  ): IO[Response[IO]] =
    val (problemSlug, filename) = input
    ProblemQueryTable.findBySlug(connection, problemSlug).flatMap {
      case None =>
        HttpApiError.raise(HttpApiError.notFound(ApiMessages.problemNotFound))
      case Some(problem) =>
        for
          canManage <- ProblemAccessRules.canManageProblem(connection, actor, problem)
          _ <- HttpApiError.ensure(canManage, HttpApiError.notFound(ApiMessages.problemNotFound))
          response <- problemDataStorage.readFile(problem.slug, filename).flatMap {
            case None =>
              HttpApiError.raise(HttpApiError.notFound(ApiMessages.problemDataFileNotFound))
            case Some((sanitizedFilename, bytes)) =>
              IO.pure(binaryResponse(sanitizedFilename.value, bytes))
          }
        yield response
    }

  private def binaryResponse(filename: String, bytes: Array[Byte]): Response[IO] =
    Response[IO](status = Status.Ok)
      .putHeaders(
        Header.Raw(CIString("Content-Type"), "application/octet-stream"),
        Header.Raw(CIString("Content-Disposition"), s"""attachment; filename="$filename""""),
        Header.Raw(CIString("Content-Length"), bytes.length.toString)
      )
      .withBodyStream(Stream.emits(bytes).covary[IO])
