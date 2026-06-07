package domains.rating.api

import cats.effect.IO
import domains.auth.api.SiteManagerApi
import domains.auth.objects.SiteManagerUser
import domains.rating.objects.response.RatingManageState
import domains.rating.table.rating.RatingTable
import io.circe.Encoder
import org.http4s.{Method, Request, Status}
import shared.api.{ApiMessages, ApiPath, HttpApiError, PathParams}

import java.sql.Connection

object PopRatingContest extends SiteManagerApi[Unit, RatingManageState]:

  override val method: Method = Method.POST
  override val path: ApiPath = ApiPath("/api/ratings/manage/contests/pop")
  override val successStatus: Status = Status.Ok
  override protected val outputEncoder: Encoder[RatingManageState] = summon[Encoder[RatingManageState]]

  override def decode(request: Request[IO], pathParams: PathParams): IO[Unit] =
    val _ = (request, pathParams)
    IO.unit

  override def plan(connection: Connection, actor: SiteManagerUser, input: Unit): IO[RatingManageState] =
    val _ = (actor, input)
    for
      deleted <- RatingTable.popLatestContest(connection)
      _ <- HttpApiError.ensure(deleted, HttpApiError.badRequest(ApiMessages.ratingSequenceEmpty))
      contests <- RatingTable.listManageContests(connection)
    yield RatingManageState(contests)
