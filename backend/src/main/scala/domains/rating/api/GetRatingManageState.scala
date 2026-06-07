package domains.rating.api

import cats.effect.IO
import domains.auth.api.SiteManagerApi
import domains.auth.objects.SiteManagerUser
import domains.rating.objects.response.RatingManageState
import domains.rating.table.rating.RatingTable
import io.circe.Encoder
import org.http4s.{Method, Request, Status}
import shared.api.{ApiPath, PathParams}

import java.sql.Connection

object GetRatingManageState extends SiteManagerApi[Unit, RatingManageState]:

  override val method: Method = Method.GET
  override val path: ApiPath = ApiPath("/api/ratings/manage")
  override val successStatus: Status = Status.Ok
  override protected val outputEncoder: Encoder[RatingManageState] = summon[Encoder[RatingManageState]]

  override def decode(request: Request[IO], pathParams: PathParams): IO[Unit] =
    val _ = (request, pathParams)
    IO.unit

  override def plan(connection: Connection, actor: SiteManagerUser, input: Unit): IO[RatingManageState] =
    val _ = (actor, input)
    RatingTable.listManageContests(connection).map(RatingManageState(_))
