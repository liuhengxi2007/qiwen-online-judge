package domains.problem.api

import cats.effect.IO
import domains.auth.api.AuthenticatedApi
import domains.auth.objects.internal.AuthenticatedUser

import domains.problem.objects.ProblemSlug
import domains.problem.objects.response.ProblemDetail
import io.circe.Encoder
import org.http4s.{Method, Request, Status}
import shared.api.{ApiMessages, ApiPath, HttpApiError, PathParams}

import java.sql.Connection

/** 获取题目详情的认证 API；会按访问策略和所属题单可见性过滤，不向无权用户泄露隐藏题目。 */
object GetProblem extends AuthenticatedApi[ProblemSlug, ProblemDetail]:

  override val method: Method = Method.GET
  override val path: ApiPath = ApiPath("/api/problems/:problemSlug")
  override val successStatus: Status = Status.Ok
  override protected val outputEncoder: Encoder[ProblemDetail] = summon[Encoder[ProblemDetail]]

  /** 从路径解析题目 slug；请求体在 GET 接口中没有业务含义。 */
  override def decode(request: Request[IO], pathParams: PathParams): IO[ProblemSlug] =
    val _ = request
    HttpApiError.fromEitherBadRequest(pathParams.require("problemSlug").flatMap(ProblemSlug.parse))

  /** 返回可见题目详情并附带 canManage；不可见或不存在统一返回题目不存在。 */
  override def plan(
    connection: Connection,
    actor: AuthenticatedUser,
    problemSlug: ProblemSlug
  ): IO[ProblemDetail] =
    EvaluateProblemAccess.plan(connection, actor, problemSlug).flatMap { access =>
      access.problem match
        case Some(problem) if access.canView =>
          IO.pure(problem.copy(canManage = access.canManage))
        case _ =>
          HttpApiError.raise(HttpApiError.notFound(ApiMessages.problemNotFound))
    }
