package domains.problem.api

import cats.effect.IO
import domains.auth.api.AuthenticatedResponseApi
import domains.auth.objects.internal.AuthenticatedUser
import domains.problem.utils.ProblemDataStorage
import domains.problem.objects.{ProblemDataPath, ProblemSlug}
import fs2.Stream
import org.http4s.{Header, Method, Request, Response, Status}
import org.typelevel.ci.CIString
import shared.api.{ApiMessages, ApiPath, HttpApiError, PathParams}

import java.sql.Connection

final case class DownloadProblemDataPath(problemDataStorage: ProblemDataStorage)
    extends AuthenticatedResponseApi[(ProblemSlug, ProblemDataPath)]:

  override val method: Method = Method.GET
  override val path: ApiPath = ApiPath("/api/problems/:problemSlug/data/files/download")

  override def decode(request: Request[IO], pathParams: PathParams): IO[(ProblemSlug, ProblemDataPath)] =
    HttpApiError.fromEitherBadRequest(
      for
        problemSlug <- pathParams.require("problemSlug").flatMap(ProblemSlug.parse)
        rawPath <- request.uri.query.params.get("path").toRight("Missing query parameter: path.")
        path <- ProblemDataPath.parse(rawPath)
      yield (problemSlug, path)
    )

  override def plan(
    connection: Connection,
    actor: AuthenticatedUser,
    input: (ProblemSlug, ProblemDataPath)
  ): IO[Response[IO]] =
    val (problemSlug, path) = input
    EvaluateProblemAccess.plan(connection, actor, problemSlug).flatMap { access =>
      access.problem match
        case None =>
          HttpApiError.raise(HttpApiError.notFound(ApiMessages.problemNotFound))
        case Some(problem) =>
          HttpApiError.ensure(access.canManage, HttpApiError.notFound(ApiMessages.problemNotFound)) *>
            downloadManagedProblemDataPath(problem, path)
    }

  def downloadManagedProblemDataPath(problem: domains.problem.objects.response.ProblemDetail, path: ProblemDataPath): IO[Response[IO]] =
    problemDataStorage.readPath(problem.slug, path).flatMap {
      case None =>
        HttpApiError.raise(HttpApiError.notFound(ApiMessages.problemDataFileNotFound))
      case Some((storedPath, bytes)) =>
        IO.pure(binaryResponse(storedPath.fileName, bytes))
    }

  private def binaryResponse(filename: String, bytes: Array[Byte]): Response[IO] =
    Response[IO](status = Status.Ok)
      .putHeaders(
        Header.Raw(CIString("Content-Type"), "application/octet-stream"),
        Header.Raw(CIString("Content-Disposition"), s"""attachment; filename="$filename""""),
        Header.Raw(CIString("Content-Length"), bytes.length.toString)
      )
      .withBodyStream(Stream.emits(bytes).covary[IO])
