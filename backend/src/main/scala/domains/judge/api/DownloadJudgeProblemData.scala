package domains.judge.api

import cats.effect.IO
import domains.auth.api.PublicResponseApi
import domains.judge.utils.JudgeConfig
import domains.judge.utils.JudgeTokenAuth
import domains.hack.api.ReadHackProblemData
import domains.problem.utils.ProblemDataStorage
import domains.problem.objects.{ProblemDataPath, ProblemSlug}
import fs2.Stream
import org.http4s.{Header, Method, Request, Response, Status}
import org.typelevel.ci.CIString
import shared.api.{ApiMessages, ApiPath, HttpApiError, PathParams}

import java.sql.Connection

final case class DownloadJudgeProblemData(
  judgeConfig: JudgeConfig,
  problemDataStorage: ProblemDataStorage
) extends PublicResponseApi[(ProblemSlug, ProblemDataPath)]:

  override val method: Method = Method.GET
  override val path: ApiPath = ApiPath("/api/worker/judge/problem-data")

  override def decode(request: Request[IO], pathParams: PathParams): IO[(ProblemSlug, ProblemDataPath)] =
    val _ = pathParams
    for
      _ <- JudgeTokenAuth.ensureJudgeToken(request, judgeConfig)
      problemSlug <- HttpApiError.fromEitherBadRequest(
        request.uri.query.params.get("problemSlug").toRight("Valid problemSlug and path query parameters are required.").flatMap(ProblemSlug.parse)
      )
      path <- HttpApiError.fromEitherBadRequest(
        request.uri.query.params.get("path").toRight("Valid problemSlug and path query parameters are required.").flatMap(ProblemDataPath.parse)
      )
    yield (problemSlug, path)

  override def plan(connection: Connection, input: (ProblemSlug, ProblemDataPath)): IO[Response[IO]] =
    val (problemSlug, path) = input
    ReadHackProblemData.plan(connection, ReadHackProblemData.input(problemSlug, path.value)).flatMap {
      case Some((filename, bytes)) =>
        IO.pure(bytesResponse(filename, bytes))
      case None =>
        problemDataStorage.readPath(problemSlug, path).flatMap {
          case None =>
            HttpApiError.raise(HttpApiError.notFound(ApiMessages.problemDataFileNotFound))
          case Some((storedPath, bytes)) =>
            IO.pure(bytesResponse(storedPath.fileName, bytes))
        }
    }

  private def bytesResponse(filename: String, bytes: Array[Byte]): Response[IO] =
    Response[IO](status = Status.Ok)
      .putHeaders(
        Header.Raw(CIString("Content-Type"), "application/octet-stream"),
        Header.Raw(CIString("Content-Disposition"), s"""attachment; filename="$filename""""),
        Header.Raw(CIString("Content-Length"), bytes.length.toString)
      )
      .withBodyStream(Stream.emits(bytes).covary[IO])
