package domains.problemset.api

import cats.effect.IO
import domains.auth.api.AuthenticatedApi
import domains.auth.objects.internal.AuthenticatedUser

import domains.problemset.objects.ProblemSetSlug
import domains.problemset.utils.ProblemSetAccessRules
import domains.problemset.table.problem_set.ProblemSetTable
import domains.problemset.table.problem_set_access_grant.ProblemSetAccessGrantTable
import io.circe.Encoder
import org.http4s.{Method, Request, Status}
import shared.api.{ApiMessages, ApiPath, HttpApiError, PathParams}
import shared.objects.response.SuccessResponse

import java.sql.Connection

/** 删除题单的认证 API，仅题目管理员可调用，并清理题单访问授权。 */
object DeleteProblemSet extends AuthenticatedApi[ProblemSetSlug, SuccessResponse]:

  override val method: Method = Method.POST
  override val path: ApiPath = ApiPath("/api/problem-sets/:problemSetSlug/delete")
  override val successStatus: Status = Status.Ok
  override protected val outputEncoder: Encoder[SuccessResponse] = summon[Encoder[SuccessResponse]]

  /** 从路径解析题单 slug，删除入口不读取请求体。 */
  override def decode(request: Request[IO], pathParams: PathParams): IO[ProblemSetSlug] =
    val _ = request
    HttpApiError.fromEitherBadRequest(pathParams.require("problemSetSlug").flatMap(ProblemSetSlug.parse))

  /** 校验管理权限和题单存在性，先删除授权再删除题单主体。 */
  override def plan(
    connection: Connection,
    actor: AuthenticatedUser,
    problemSetSlug: ProblemSetSlug
  ): IO[SuccessResponse] =
    for
      /** 注意：非题目管理员返回 404，用于隐藏题单管理入口和题单存在性。 */
      _ <- HttpApiError.ensure(
        ProblemSetAccessRules.canManageProblemSets(actor),
        HttpApiError.notFound(ApiMessages.problemSetNotFound)
      )
      maybeProblemSet <- ProblemSetTable.findBySlug(connection, problemSetSlug)
      problemSet <- maybeProblemSet match
        case Some(problemSet) => IO.pure(problemSet)
        case None => HttpApiError.raise(HttpApiError.notFound(ApiMessages.problemSetNotFound))
      _ <- ProblemSetAccessGrantTable.deleteAllForProblemSet(connection, problemSet.id)
      _ <- ProblemSetTable.delete(connection, problemSet.id)
    yield SuccessResponse(code = Some(ApiMessages.problemSetDeleted.code), message = None, params = ApiMessages.problemSetDeleted.params)
