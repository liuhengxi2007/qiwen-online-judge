package domains.auth.api

import cats.effect.IO
import database.DatabaseSession
import domains.auth.objects.SiteManagerUser
import domains.auth.objects.internal.AuthenticatedUser
import domains.auth.utils.SessionStoreContext
import io.circe.Encoder
import io.circe.syntax.*
import org.http4s.circe.CirceEntityEncoder.*
import org.http4s.{Method, Request, Response, Status}
import shared.api.{ApiMessages, ApiPath, HttpApiError, PathParams}

import java.sql.Connection

/** API 执行上下文，携带数据库事务入口和可选的会话解析器。 */
final case class ApiObjectContext(
  databaseSession: DatabaseSession,
  sessionStore: Option[SessionStoreContext]
)

/** API 上下文构造器，区分需要登录态解析器的普通上下文和纯公开上下文。 */
object ApiObjectContext:
  /** 构造带会话解析器的上下文，供登录态或管理员 API 使用。 */
  def apply(databaseSession: DatabaseSession, sessionStore: SessionStoreContext): ApiObjectContext =
    ApiObjectContext(databaseSession, Some(sessionStore))

  /** 构造无会话解析器的上下文，仅适用于公开 API。 */
  def public(databaseSession: DatabaseSession): ApiObjectContext =
    ApiObjectContext(databaseSession, None)

/** 统一 API 对象抽象，声明 HTTP 方法、路径和路由器调用入口；API 对齐例外：本文件是后端传输协议基础设施，不是可调用端点文件。 */
trait ApiObject:
  /** HTTP 方法，用于路由匹配。 */
  def method: Method
  /** API 路径模式，用于路由匹配和路径参数提取。 */
  def path: ApiPath

  private[api] def handle(
    context: ApiObjectContext,
    request: Request[IO],
    pathParams: PathParams
  ): IO[Response[IO]]

/** 公开 JSON API 模板，在事务中 decode 输入、执行业务 plan 并编码 JSON 输出。 */
trait PublicApi[Input, Output] extends ApiObject:
  /** 成功响应状态码，默认 200。 */
  def successStatus: Status = Status.Ok
  protected def outputEncoder: Encoder[Output]

  /** 将 HTTP 请求和路径参数解码为业务输入，失败通常映射为 400。 */
  def decode(request: Request[IO], pathParams: PathParams): IO[Input]

  /** 在数据库事务连接中执行业务逻辑并返回 JSON 输出模型。 */
  def plan(connection: Connection, input: Input): IO[Output]

  override private[api] final def handle(
    context: ApiObjectContext,
    request: Request[IO],
    pathParams: PathParams
  ): IO[Response[IO]] =
    for
      input <- decode(request, pathParams)
      response <- context.databaseSession.withTransactionConnection { connection =>
        for
          output <- plan(connection, input)
          response <- jsonResponse(output)
        yield response
      }
    yield response

  protected final def jsonResponse(output: Output): IO[Response[IO]] =
    IO.pure(Response[IO](status = successStatus).withEntity(output.asJson(using outputEncoder)))

/** 公开响应 API 模板，允许 plan 直接构造完整 Response，例如设置 cookie。 */
trait PublicResponseApi[Input] extends ApiObject:
  /** 将 HTTP 请求和路径参数解码为业务输入。 */
  def decode(request: Request[IO], pathParams: PathParams): IO[Input]

  /** 在事务中执行业务逻辑并返回完整 HTTP 响应。 */
  def plan(connection: Connection, input: Input): IO[Response[IO]]

  override private[api] final def handle(
    context: ApiObjectContext,
    request: Request[IO],
    pathParams: PathParams
  ): IO[Response[IO]] =
    for
      input <- decode(request, pathParams)
      response <- context.databaseSession.withTransactionConnection { connection =>
        plan(connection, input)
      }
    yield response

/** 内部 API 模板，只允许代码直接调用 plan，外部 HTTP 调用固定返回 forbidden。 */
trait InternalOnlyApi[Input, Output] extends ApiObject:
  /** 内部业务入口，调用方负责提供已解析输入和事务连接。 */
  def plan(connection: Connection, input: Input): IO[Output]

  override private[api] final def handle(
    context: ApiObjectContext,
    request: Request[IO],
    pathParams: PathParams
  ): IO[Response[IO]] =
    /** 注意：内部 API 只允许代码直接调用 plan，这里忽略 HTTP 上下文并固定拒绝外部访问。 */
    val _ = (context, request, pathParams)
    HttpApiError.forbidden(ApiMessages.internalApiNotCallable).toResponse

/** 登录态 JSON API 模板，进入 plan 前会解析当前认证用户。 */
trait AuthenticatedApi[Input, Output] extends ApiObject:
  /** 成功响应状态码，默认 200。 */
  def successStatus: Status = Status.Ok
  protected def outputEncoder: Encoder[Output]

  /** 将 HTTP 请求和路径参数解码为业务输入，尚不做登录态解析。 */
  def decode(request: Request[IO], pathParams: PathParams): IO[Input]

  /** 在事务中以当前认证用户身份执行业务逻辑并返回 JSON 输出。 */
  def plan(connection: Connection, actor: AuthenticatedUser, input: Input): IO[Output]

  override private[api] final def handle(
    context: ApiObjectContext,
    request: Request[IO],
    pathParams: PathParams
  ): IO[Response[IO]] =
    for
      input <- decode(request, pathParams)
      response <- context.databaseSession.withTransactionConnection { connection =>
        for
          actor <- SessionResolver.resolveAuthenticatedUser(requiredSessionStore(context), connection, request)
          output <- plan(connection, actor, input)
          response <- jsonResponse(output)
        yield response
      }
    yield response

  protected final def jsonResponse(output: Output): IO[Response[IO]] =
    IO.pure(Response[IO](status = successStatus).withEntity(output.asJson(using outputEncoder)))

/** 站点管理员 JSON API 模板，进入 plan 前会校验当前用户为 site manager。 */
trait SiteManagerApi[Input, Output] extends ApiObject:
  /** 成功响应状态码，默认 200。 */
  def successStatus: Status = Status.Ok
  protected def outputEncoder: Encoder[Output]

  /** 将 HTTP 请求和路径参数解码为业务输入，权限校验由模板完成。 */
  def decode(request: Request[IO], pathParams: PathParams): IO[Input]

  /** 在事务中以站点管理员身份执行业务逻辑并返回 JSON 输出。 */
  def plan(connection: Connection, actor: SiteManagerUser, input: Input): IO[Output]

  override private[api] final def handle(
    context: ApiObjectContext,
    request: Request[IO],
    pathParams: PathParams
  ): IO[Response[IO]] =
    for
      input <- decode(request, pathParams)
      response <- context.databaseSession.withTransactionConnection { connection =>
        for
          actor <- SessionResolver.resolveSiteManager(requiredSessionStore(context), connection, request)
          output <- plan(connection, actor, input)
          response <- jsonResponse(output)
        yield response
      }
    yield response

  protected final def jsonResponse(output: Output): IO[Response[IO]] =
    IO.pure(Response[IO](status = successStatus).withEntity(output.asJson(using outputEncoder)))

/** 登录态响应 API 模板，适合需要直接返回流、cookie 或非 JSON 细节的接口。 */
trait AuthenticatedResponseApi[Input] extends ApiObject:
  /** 将 HTTP 请求和路径参数解码为业务输入。 */
  def decode(request: Request[IO], pathParams: PathParams): IO[Input]

  /** 在事务中以当前认证用户身份执行业务逻辑并返回完整 HTTP 响应。 */
  def plan(connection: Connection, actor: AuthenticatedUser, input: Input): IO[Response[IO]]

  override private[api] final def handle(
    context: ApiObjectContext,
    request: Request[IO],
    pathParams: PathParams
  ): IO[Response[IO]] =
    for
      input <- decode(request, pathParams)
      response <- context.databaseSession.withTransactionConnection { connection =>
        for
          actor <- SessionResolver.resolveAuthenticatedUser(requiredSessionStore(context), connection, request)
          response <- plan(connection, actor, input)
        yield response
      }
    yield response

/** 需要认证用户输入的内部 API 模板，外部 HTTP 调用固定返回 forbidden。 */
trait InternalOnlyAuthenticatedApi[Input, Output] extends ApiObject:
  /** 内部业务入口，调用方负责提供认证用户和事务连接。 */
  def plan(connection: Connection, actor: AuthenticatedUser, input: Input): IO[Output]

  override private[api] final def handle(
    context: ApiObjectContext,
    request: Request[IO],
    pathParams: PathParams
  ): IO[Response[IO]] =
    /** 注意：内部认证 API 只允许代码直接调用 plan，这里忽略 HTTP 上下文并固定拒绝外部访问。 */
    val _ = (context, request, pathParams)
    HttpApiError.forbidden(ApiMessages.internalApiNotCallable).toResponse

/** 从上下文取出会话存储；认证模板配置错误时抛出状态异常。 */
private def requiredSessionStore(context: ApiObjectContext): SessionStoreContext =
  context.sessionStore.getOrElse(throw new IllegalStateException("Session store is required for authenticated API objects."))
