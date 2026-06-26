package domains.problem.api

import cats.effect.IO
import domains.auth.api.AuthenticatedApi
import domains.auth.objects.internal.AuthenticatedUser

import domains.problem.objects.response.ProblemDataFileListResponse
import io.circe.Encoder
import org.http4s.{Method, Request, Status}
import shared.api.{ApiPath, PathParams}

import java.sql.Connection

/** 列出题目数据文件名的管理端 API；从对象存储读取当前文件列表，要求题目管理权限。 */
final case class ListProblemDataFiles(problemDataStorage: ProblemDataStorageContext)
    extends AuthenticatedApi[ProblemManagementContext, ProblemDataFileListResponse]:

  override val method: Method = Method.GET
  override val path: ApiPath = ApiPath("/api/problems/:problemSlug/data/files")
  override val successStatus: Status = Status.Ok
  override protected val outputEncoder: Encoder[ProblemDataFileListResponse] = summon[Encoder[ProblemDataFileListResponse]]

  /** 解析题目管理上下文。 */
  override def decode(request: Request[IO], pathParams: PathParams): IO[ProblemManagementContext] =
    ProblemManagementContext.decode(request, pathParams)

  /** 校验管理权限后列出对象存储中的文件名；目录路径会在存储扩展方法中折叠为文件名。 */
  override def plan(
    connection: Connection,
    actor: AuthenticatedUser,
    context: ProblemManagementContext
  ): IO[ProblemDataFileListResponse] =
    ProblemManagementContext
      .requireManagedProblem(connection, actor, context)
      .flatMap(problem => ProblemDataStorage.listFiles(problemDataStorage, problem.slug).map(ProblemDataFileListResponse(_)))
