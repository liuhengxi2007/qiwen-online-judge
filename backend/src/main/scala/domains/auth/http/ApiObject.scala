package domains.auth.http

import cats.effect.IO
import database.DatabaseSession
import domains.auth.objects.{AuthUser, SiteManagerUser}
import io.circe.Encoder
import io.circe.syntax.*
import org.http4s.circe.CirceEntityEncoder.*
import org.http4s.{Method, Request, Response, Status}
import shared.http.{ApiPath, PathParams}

import java.sql.Connection

final case class ApiObjectContext(
  databaseSession: DatabaseSession,
  sessionResolver: Option[SessionResolver]
)

object ApiObjectContext:
  def apply(databaseSession: DatabaseSession, sessionResolver: SessionResolver): ApiObjectContext =
    ApiObjectContext(databaseSession, Some(sessionResolver))

  def public(databaseSession: DatabaseSession): ApiObjectContext =
    ApiObjectContext(databaseSession, None)

trait ApiObject:
  def method: Method
  def path: ApiPath

  private[http] def handle(
    context: ApiObjectContext,
    request: Request[IO],
    pathParams: PathParams
  ): IO[Response[IO]]

trait PublicApi[Input, Output] extends ApiObject:
  def successStatus: Status = Status.Ok
  protected def outputEncoder: Encoder[Output]

  def decode(request: Request[IO], pathParams: PathParams): IO[Input]

  def plan(connection: Connection, input: Input): IO[Output]

  override private[http] final def handle(
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

trait PublicResponseApi[Input] extends ApiObject:
  def decode(request: Request[IO], pathParams: PathParams): IO[Input]

  def plan(connection: Connection, input: Input): IO[Response[IO]]

  override private[http] final def handle(
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

trait AuthenticatedApi[Input, Output] extends ApiObject:
  def successStatus: Status = Status.Ok
  protected def outputEncoder: Encoder[Output]

  def decode(request: Request[IO], pathParams: PathParams): IO[Input]

  def plan(connection: Connection, actor: AuthUser, input: Input): IO[Output]

  override private[http] final def handle(
    context: ApiObjectContext,
    request: Request[IO],
    pathParams: PathParams
  ): IO[Response[IO]] =
    for
      input <- decode(request, pathParams)
      response <- context.databaseSession.withTransactionConnection { connection =>
        for
          actor <- requiredSessionResolver(context).resolveAuthUser(connection, request)
          output <- plan(connection, actor, input)
          response <- jsonResponse(output)
        yield response
      }
    yield response

  protected final def jsonResponse(output: Output): IO[Response[IO]] =
    IO.pure(Response[IO](status = successStatus).withEntity(output.asJson(using outputEncoder)))

trait SiteManagerApi[Input, Output] extends ApiObject:
  def successStatus: Status = Status.Ok
  protected def outputEncoder: Encoder[Output]

  def decode(request: Request[IO], pathParams: PathParams): IO[Input]

  def plan(connection: Connection, actor: SiteManagerUser, input: Input): IO[Output]

  override private[http] final def handle(
    context: ApiObjectContext,
    request: Request[IO],
    pathParams: PathParams
  ): IO[Response[IO]] =
    for
      input <- decode(request, pathParams)
      response <- context.databaseSession.withTransactionConnection { connection =>
        for
          actor <- requiredSessionResolver(context).resolveSiteManager(connection, request)
          output <- plan(connection, actor, input)
          response <- jsonResponse(output)
        yield response
      }
    yield response

  protected final def jsonResponse(output: Output): IO[Response[IO]] =
    IO.pure(Response[IO](status = successStatus).withEntity(output.asJson(using outputEncoder)))

trait AuthenticatedResponseApi[Input] extends ApiObject:
  def decode(request: Request[IO], pathParams: PathParams): IO[Input]

  def plan(connection: Connection, actor: AuthUser, input: Input): IO[Response[IO]]

  override private[http] final def handle(
    context: ApiObjectContext,
    request: Request[IO],
    pathParams: PathParams
  ): IO[Response[IO]] =
    for
      input <- decode(request, pathParams)
      response <- context.databaseSession.withTransactionConnection { connection =>
        for
          actor <- requiredSessionResolver(context).resolveAuthUser(connection, request)
          response <- plan(connection, actor, input)
        yield response
      }
    yield response

private def requiredSessionResolver(context: ApiObjectContext): SessionResolver =
  context.sessionResolver.getOrElse(throw new IllegalStateException("Session resolver is required for authenticated API objects."))
