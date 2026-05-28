package domains.judge.http.api

import cats.effect.IO
import domains.auth.http.PublicResponseApi
import domains.judge.application.JudgeConfig
import domains.judge.http.JudgeApiSupport
import domains.problem.application.ProblemDataStorage
import domains.problem.objects.{ProblemDataPath, ProblemSlug}
import fs2.Stream
import org.http4s.{Header, Method, Request, Response, Status}
import org.typelevel.ci.CIString
import shared.http.{ApiMessages, ApiPath, HttpApiError, PathParams}

import java.sql.Connection

final case class DownloadJudgeProblemData(
  judgeConfig: JudgeConfig,
  problemDataStorage: ProblemDataStorage
) extends PublicResponseApi[(ProblemSlug, ProblemDataPath)]:

  override val method: Method = Method.GET
  override val path: ApiPath = ApiPath("/api/internal/judge/problem-data")

  override def decode(request: Request[IO], pathParams: PathParams): IO[(ProblemSlug, ProblemDataPath)] =
    val _ = pathParams
    for
      _ <- JudgeApiSupport.ensureJudgeToken(request, judgeConfig)
      problemSlug <- HttpApiError.fromEitherBadRequest(
        request.uri.query.params.get("problemSlug").toRight("Valid problemSlug and path query parameters are required.").flatMap(ProblemSlug.parse)
      )
      path <- HttpApiError.fromEitherBadRequest(
        request.uri.query.params.get("path").toRight("Valid problemSlug and path query parameters are required.").flatMap(ProblemDataPath.parse)
      )
    yield (problemSlug, path)

  override def plan(connection: Connection, input: (ProblemSlug, ProblemDataPath)): IO[Response[IO]] =
    val _ = connection
    val (problemSlug, path) = input
    problemDataStorage.readPath(problemSlug, path).flatMap {
      case None =>
        HttpApiError.raise(HttpApiError.notFound(ApiMessages.problemDataFileNotFound))
      case Some((storedPath, bytes)) =>
        IO.pure(
          Response[IO](status = Status.Ok)
            .putHeaders(
              Header.Raw(CIString("Content-Type"), "application/octet-stream"),
              Header.Raw(CIString("Content-Disposition"), s"""attachment; filename="${storedPath.fileName}""""),
              Header.Raw(CIString("Content-Length"), bytes.length.toString)
            )
            .withBodyStream(Stream.emits(bytes).covary[IO])
        )
    }
