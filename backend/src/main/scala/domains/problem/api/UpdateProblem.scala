package domains.problem.api

import cats.effect.IO
import domains.auth.api.{AuthenticatedApi, ResolveAccountUsername}
import domains.auth.objects.internal.AuthenticatedUser

import domains.problem.objects.*
import domains.problem.objects.request.UpdateProblemRequest
import domains.problem.objects.response.ProblemDetail
import domains.problem.table.problem.{ProblemMutationTable, ProblemQueryTable}
import io.circe.Encoder
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.{Method, Request, Status}
import shared.api.{ApiMessages, ApiPath, HttpApiError, PathParams}

import java.sql.Connection
import java.time.Instant

/** 更新题目元信息与访问策略的管理端 API；不修改题目数据文件和判题配置。 */
object UpdateProblem extends AuthenticatedApi[(ProblemManagementContext, UpdateProblemRequest), ProblemDetail]:

  override val method: Method = Method.POST
  override val path: ApiPath = ApiPath("/api/problems/:problemSlug")
  override val successStatus: Status = Status.Ok
  override protected val outputEncoder: Encoder[ProblemDetail] = summon[Encoder[ProblemDetail]]

  /** 解析题目管理上下文和更新请求体。 */
  override def decode(request: Request[IO], pathParams: PathParams): IO[(ProblemManagementContext, UpdateProblemRequest)] =
    for
      context <- ProblemManagementContext.decode(request, pathParams)
      body <- request.as[UpdateProblemRequest]
    yield (context, body)

  /** 校验请求字段与管理权限后执行更新；输出刷新后的可管理题目详情。 */
  override def plan(
    connection: Connection,
    actor: AuthenticatedUser,
    input: (ProblemManagementContext, UpdateProblemRequest)
  ): IO[ProblemDetail] =
    val (context, request) = input
    for
      validRequest <- validateRequest(request)
      problem <- ProblemManagementContext.requireManagedProblem(connection, actor, context)
      updatedProblem <- updateManagedProblem(connection, problem, validRequest)
    yield updatedProblem

  /** 复用领域类型校验标题和题面，输出规范化后的请求。 */
  def validateRequest(request: UpdateProblemRequest): IO[UpdateProblemRequest] =
    for
      title <- HttpApiError.fromEitherBadRequest(ProblemTitle.parse(request.title.value))
      statement <- HttpApiError.fromEitherBadRequest(ProblemStatementText.parse(request.statement.value))
    yield request.copy(
      title = title,
      statement = statement
    )

  /** 更新已确认可管理的题目；会校验作者账号和访问策略主体，并重写授权 grants。 */
  def updateManagedProblem(connection: Connection, problem: ProblemDetail, request: UpdateProblemRequest): IO[ProblemDetail] =
    for
      _ <- validateAuthorUsername(connection, request.authorUsername)
      _ <- ProblemAccessPolicyValidation.validateAccessPolicySubjects(connection, request.accessPolicy)
      _ <- ProblemMutationTable.update(connection, problem.id, Instant.now(), request)
      updatedProblem <- ProblemQueryTable.findBySlug(connection, problem.slug).flatMap {
        case Some(problem) => IO.pure(problem.copy(canManage = true))
        case None => HttpApiError.raise(HttpApiError.internal("Problem disappeared after update."))
      }
    yield updatedProblem

  private def validateAuthorUsername(connection: Connection, authorUsername: Option[domains.user.objects.Username]): IO[Unit] =
    authorUsername match
      case Some(username) =>
        ResolveAccountUsername.plan(connection, username).flatMap { user =>
          HttpApiError.ensure(user.username.nonEmpty, HttpApiError.badRequest(ApiMessages.userNotFound))
        }
      case None =>
        IO.unit
