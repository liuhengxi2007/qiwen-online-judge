package domains.problem.api

import cats.effect.IO
import domains.auth.api.AuthenticatedApi
import domains.auth.objects.internal.AuthenticatedUser

import domains.problem.objects.response.ProblemDataTreeResponse
import domains.problem.table.problem_data_file.ProblemDataFileTable
import io.circe.Encoder
import org.http4s.{Method, Request, Status}
import shared.api.{ApiPath, PathParams}

import java.sql.Connection

/** 读取题目数据目录树的管理端 API；基于数据库清单构造目录和文件节点。 */
object ListProblemDataTree extends AuthenticatedApi[ProblemManagementContext, ProblemDataTreeResponse]:

  override val method: Method = Method.GET
  override val path: ApiPath = ApiPath("/api/problems/:problemSlug/data/files/tree")
  override val successStatus: Status = Status.Ok
  override protected val outputEncoder: Encoder[ProblemDataTreeResponse] = summon[Encoder[ProblemDataTreeResponse]]

  /** 解析题目管理上下文。 */
  override def decode(request: Request[IO], pathParams: PathParams): IO[ProblemManagementContext] =
    ProblemManagementContext.decode(request, pathParams)

  /** 校验管理权限后返回题目数据树；不会直接扫描对象存储。 */
  override def plan(
    connection: Connection,
    actor: AuthenticatedUser,
    context: ProblemManagementContext
  ): IO[ProblemDataTreeResponse] =
    ProblemManagementContext
      .requireManagedProblem(connection, actor, context)
      .flatMap(problem => listManagedProblemDataTree(connection, problem))

  /** 将已授权题目的数据清单转换成树形响应，输出包含隐式目录节点。 */
  def listManagedProblemDataTree(connection: Connection, problem: domains.problem.objects.response.ProblemDetail): IO[ProblemDataTreeResponse] =
    ProblemDataFileTable.manifestForProblem(connection, problem.id, problem.slug).map(manifest => ProblemDataApiHelpers.buildTreeResponse(manifest.entries))
