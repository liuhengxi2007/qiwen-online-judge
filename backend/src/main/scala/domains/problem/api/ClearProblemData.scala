package domains.problem.api

import cats.effect.IO
import domains.auth.api.AuthenticatedApi
import domains.auth.objects.internal.AuthenticatedUser
import domains.problem.utils.ProblemDataStorage
import domains.problem.utils.ProblemDataApiHelpers

import domains.problem.objects.ProblemSlug
import domains.problem.objects.response.ProblemDetail
import domains.problem.table.problem.ProblemDataStateTable
import domains.problem.table.problem_data_file.ProblemDataFileTable
import io.circe.Encoder
import org.http4s.{Method, Request, Status}
import shared.api.{ApiPath, PathParams}

import java.sql.Connection
import java.time.Instant

/** 清空题目数据目录的管理端 API；要求调用者具备题目管理权限，副作用是删除对象存储文件、清空清单表并更新题目数据状态。 */
final case class ClearProblemData(problemDataStorage: ProblemDataStorage)
    extends AuthenticatedApi[ProblemManagementContext, ProblemDetail]:

  override val method: Method = Method.POST
  override val path: ApiPath = ApiPath("/api/problems/:problemSlug/data/files/delete-all")
  override val successStatus: Status = Status.Ok
  override protected val outputEncoder: Encoder[ProblemDetail] = summon[Encoder[ProblemDetail]]

  /** 从路径和可选竞赛上下文解析题目管理目标；只做输入边界校验，不访问存储。 */
  override def decode(request: Request[IO], pathParams: PathParams): IO[ProblemManagementContext] =
    ProblemManagementContext.decode(request, pathParams)

  /** 校验管理权限后清空题目数据，输出刷新后的题目详情；权限失败对直接题目场景隐藏资源存在性。 */
  override def plan(
    connection: Connection,
    actor: AuthenticatedUser,
    context: ProblemManagementContext
  ): IO[ProblemDetail] =
    ProblemManagementContext
      .requireManagedProblem(connection, actor, context)
      .flatMap(_ => clearManagedProblemData(connection, context.problemSlug))

  /** 在持有题目行锁后删除所有题目数据文件；失败时用对象存储快照尝试恢复，数据库事务由外层会话负责。 */
  def clearManagedProblemData(connection: Connection, problemSlug: ProblemSlug): IO[ProblemDetail] =
    ProblemDataApiHelpers.withProblemForUpdate(connection, problemSlug) { problem =>
      for
        snapshot <- problemDataStorage.snapshotDirectory(problem.slug)
        clearedProblem <- problemDataStorage
          .deleteAllFiles(problem.slug)
          .flatMap(_ => ProblemDataFileTable.deleteAllForProblem(connection, problem.id))
          .flatMap(_ => ProblemDataStateTable.updateData(connection, problem.id, Instant.now(), None))
          .flatMap(_ => ProblemDataApiHelpers.refreshedManagedProblem(connection, problem, "Problem disappeared after clearing data."))
          .handleErrorWith { error =>
            problemDataStorage.restoreDirectory(problem.slug, snapshot) *> IO.raiseError(error)
          }
      yield clearedProblem
    }
