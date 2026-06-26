package domains.problem.api

import cats.effect.IO
import cats.syntax.all.*
import domains.auth.api.AuthenticatedApi
import domains.auth.objects.internal.AuthenticatedUser
import domains.problem.utils.{ProblemDataStorage, ProblemDataStorageContext}

import domains.problem.objects.ProblemSlug
import domains.problem.objects.request.DeleteProblemDataPathRequest
import domains.problem.objects.response.ProblemDetail
import domains.problem.table.problem.ProblemDataStateTable
import domains.problem.table.problem_data_file.ProblemDataFileTable
import io.circe.Encoder
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.{Method, Request, Status}
import shared.api.{ApiMessages, ApiPath, HttpApiError, PathParams}

import java.sql.Connection
import java.time.Instant

/** 删除题目数据中单个路径或目录子树的管理端 API；要求题目管理权限并同步对象存储与清单表。 */
final case class DeleteProblemDataPath(problemDataStorage: ProblemDataStorageContext)
    extends AuthenticatedApi[(ProblemManagementContext, DeleteProblemDataPathRequest), ProblemDetail]:

  override val method: Method = Method.POST
  override val path: ApiPath = ApiPath("/api/problems/:problemSlug/data/files/delete")
  override val successStatus: Status = Status.Ok
  override protected val outputEncoder: Encoder[ProblemDetail] = summon[Encoder[ProblemDetail]]

  /** 解析题目管理上下文和待删除路径；路径语义由 ProblemDataPath 保证不会越界。 */
  override def decode(request: Request[IO], pathParams: PathParams): IO[(ProblemManagementContext, DeleteProblemDataPathRequest)] =
    for
      context <- ProblemManagementContext.decode(request, pathParams)
      body <- request.as[DeleteProblemDataPathRequest]
    yield (context, body)

  /** 校验管理权限后删除路径，输出刷新后的题目详情；不存在的目标路径返回文件未找到。 */
  override def plan(
    connection: Connection,
    actor: AuthenticatedUser,
    input: (ProblemManagementContext, DeleteProblemDataPathRequest)
  ): IO[ProblemDetail] =
    val (context, request) = input
    ProblemManagementContext
      .requireManagedProblem(connection, actor, context)
      .flatMap(_ => deleteManagedProblemDataPath(connection, context.problemSlug, request))

  /** 在题目行锁内删除一个文件或目录下所有文件；对象存储失败时按删除前快照恢复。 */
  def deleteManagedProblemDataPath(
    connection: Connection,
    problemSlug: ProblemSlug,
    request: DeleteProblemDataPathRequest
  ): IO[ProblemDetail] =
    ProblemDataApiHelpers.withProblemForUpdate(connection, problemSlug) { problem =>
      for
        entries <- ProblemDataFileTable.listForProblem(connection, problem.id)
        pathsToDelete = entries.map(_.path).filter(entryPath => entryPath == request.path || entryPath.value.startsWith(s"${request.path.value}/"))
        snapshot <- ProblemDataStorage.snapshotDirectory(problemDataStorage, problem.slug)
        deletedProblem <-
          if pathsToDelete.isEmpty then
            HttpApiError.raise(HttpApiError.notFound(ApiMessages.problemDataFileNotFound))
          else
            pathsToDelete
              .traverse_(pathToDelete => ProblemDataStorage.deletePath(problemDataStorage, problem.slug, pathToDelete).void)
              .flatMap(_ => pathsToDelete.traverse_(pathToDelete => ProblemDataFileTable.deleteForProblemPath(connection, problem.id, pathToDelete)))
              .flatMap(_ => ProblemDataFileTable.listForProblem(connection, problem.id))
              .flatMap(entries =>
                ProblemDataStateTable.updateData(
                  connection,
                  problem.id,
                  Instant.now(),
                  ProblemDataApiHelpers.summaryFilenameForEntries(entries)
                )
              )
              .flatMap(_ => ProblemDataApiHelpers.refreshedManagedProblem(connection, problem, "Problem disappeared after data deletion."))
              .handleErrorWith { error =>
                ProblemDataStorage.restoreDirectory(problemDataStorage, problem.slug, snapshot) *> IO.raiseError(error)
              }
      yield deletedProblem
    }
