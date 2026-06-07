package domains.rating.api

import cats.effect.IO
import domains.auth.api.AuthenticatedApi
import domains.auth.objects.internal.AuthenticatedUser
import domains.rating.objects.response.RatingRanklistItem
import domains.rating.table.rating.RatingTable
import io.circe.Encoder
import org.http4s.{Method, Request, Status}
import shared.api.utils.PageRequestQuerySupport
import shared.api.{ApiPath, PathParams}
import shared.objects.{PageRequest, PageResponse}

import java.sql.Connection

object ListRatingRanklist extends AuthenticatedApi[PageRequest, PageResponse[RatingRanklistItem]]:

  override val method: Method = Method.GET
  override val path: ApiPath = ApiPath("/api/ratings/ranklist")
  override val successStatus: Status = Status.Ok
  override protected val outputEncoder: Encoder[PageResponse[RatingRanklistItem]] = summon[Encoder[PageResponse[RatingRanklistItem]]]

  override def decode(request: Request[IO], pathParams: PathParams): IO[PageRequest] =
    val _ = pathParams
    IO.pure(PageRequestQuerySupport.parsePageRequest(request.uri.query.params))

  override def plan(
    connection: Connection,
    actor: AuthenticatedUser,
    pageRequest: PageRequest
  ): IO[PageResponse[RatingRanklistItem]] =
    val _ = actor
    RatingTable.listRanklist(connection, pageRequest)
