package domains.judge.api

import cats.effect.IO
import domains.auth.api.PublicResponseApi
import domains.judge.utils.JudgeConfig
import domains.judge.utils.JudgeTokenAuth
import domains.problem.utils.ProblemDataStorage
import domains.problem.objects.{ProblemDataPath, ProblemSlug}
import fs2.Stream
import org.http4s.{Header, Method, Request, Response, Status}
import org.typelevel.ci.CIString
import shared.api.{ApiMessages, ApiPath, HttpApiError, PathParams}

import java.sql.Connection

/** judge worker 下载题目数据单文件的公开响应 API；使用共享 token 认证，不走用户会话权限。 */
final case class DownloadJudgeProblemData(
  judgeConfig: JudgeConfig,
  problemDataStorage: ProblemDataStorage
) extends PublicResponseApi[(ProblemSlug, ProblemDataPath)]:

  override val method: Method = Method.GET
  override val path: ApiPath = ApiPath("/api/worker/judge/problem-data")

  /** 校验 worker token 并从 query 参数解析题目 slug 和数据路径。 */
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

  /** 从对象存储读取题目数据文件并返回字节流；缺失文件返回题目数据文件未找到。 */
  override def plan(connection: Connection, input: (ProblemSlug, ProblemDataPath)): IO[Response[IO]] =
    val _ = connection
    val (problemSlug, path) = input
    problemDataStorage.readPath(problemSlug, path).flatMap {
      case None =>
        HttpApiError.raise(HttpApiError.notFound(ApiMessages.problemDataFileNotFound))
      case Some((storedPath, bytes)) =>
        IO.pure(bytesResponse(storedPath.fileName, bytes))
    }

  private def bytesResponse(filename: String, bytes: Array[Byte]): Response[IO] =
    Response[IO](status = Status.Ok)
      .putHeaders(
        Header.Raw(CIString("Content-Type"), "application/octet-stream"),
        Header.Raw(CIString("Content-Disposition"), s"""attachment; filename="$filename""""),
        Header.Raw(CIString("Content-Length"), bytes.length.toString)
      )
      .withBodyStream(Stream.emits(bytes).covary[IO])
