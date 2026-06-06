package domains.hack.api

import cats.effect.IO
import domains.auth.api.AuthenticatedApi
import domains.auth.objects.internal.AuthenticatedUser
import domains.hack.objects.HackId
import domains.hack.objects.response.HackDetail
import domains.hack.table.hack.HackQueryTable
import io.circe.Encoder
import org.http4s.{Method, Request, Status}
import shared.api.{ApiMessages, ApiPath, HttpApiError, PathParams}

import java.sql.Connection

object GetHack extends AuthenticatedApi[HackId, HackDetail]:

  override val method: Method = Method.GET
  override val path: ApiPath = ApiPath("/api/hacks/:hackId")
  override val successStatus: Status = Status.Ok
  override protected val outputEncoder: Encoder[HackDetail] = summon[Encoder[HackDetail]]

  override def decode(request: Request[IO], pathParams: PathParams): IO[HackId] =
    val _ = request
    HttpApiError.fromEitherBadRequest(pathParams.require("hackId").flatMap(HackId.parse))

  override def plan(connection: Connection, actor: AuthenticatedUser, hackId: HackId): IO[HackDetail] =
    HackQueryTable.findVisibleById(connection, actor, hackId).flatMap {
      case Some(value) => IO.pure(value)
      case None => HttpApiError.raise(HttpApiError.notFound(ApiMessages.submissionNotFound))
    }
