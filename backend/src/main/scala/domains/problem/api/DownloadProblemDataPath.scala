package domains.problem.api

import cats.effect.IO
import domains.auth.api.AuthenticatedResponseApi
import domains.auth.objects.internal.AuthenticatedUser
import domains.problem.objects.ProblemDataPath
import fs2.Stream
import org.http4s.{Header, Method, Request, Response, Status}
import org.typelevel.ci.CIString
import shared.api.{ApiMessages, ApiPath, HttpApiError, PathParams}

import java.sql.Connection

/** 下载题目数据单个路径的管理端响应 API；要求题目管理权限，返回原始文件字节流。 */
final case class DownloadProblemDataPath(problemDataStorage: ProblemDataStorageContext)
    extends AuthenticatedResponseApi[(ProblemManagementContext, ProblemDataPath)]:

  override val method: Method = Method.GET
  override val path: ApiPath = ApiPath("/api/problems/:problemSlug/data/files/download")

  /** 解析题目上下文和 query 中的相对数据路径；缺失或非法路径直接拒绝。 */
  override def decode(request: Request[IO], pathParams: PathParams): IO[(ProblemManagementContext, ProblemDataPath)] =
    for
      context <- ProblemManagementContext.decode(request, pathParams)
      path <- HttpApiError.fromEitherBadRequest(
        request.uri.query.params.get("path").toRight("Missing query parameter: path.").flatMap(ProblemDataPath.parse)
      )
    yield (context, path)

  /** 校验管理权限后读取对象存储文件；文件缺失返回题目数据文件未找到。 */
  override def plan(
    connection: Connection,
    actor: AuthenticatedUser,
    input: (ProblemManagementContext, ProblemDataPath)
  ): IO[Response[IO]] =
    val (context, path) = input
    ProblemManagementContext
      .requireManagedProblem(connection, actor, context)
      .flatMap(problem => downloadManagedProblemDataPath(problem, path))

  /** 为已授权题目读取指定路径并构造下载响应；输出文件名取存储路径最后一段。 */
  def downloadManagedProblemDataPath(problem: domains.problem.objects.response.ProblemDetail, path: ProblemDataPath): IO[Response[IO]] =
    ProblemDataStorage.readPath(problemDataStorage, problem.slug, path).flatMap {
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
