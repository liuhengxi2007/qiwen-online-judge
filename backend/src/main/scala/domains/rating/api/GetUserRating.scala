package domains.rating.api

import cats.effect.IO
import domains.auth.api.InternalOnlyApi
import domains.rating.objects.RatingValue
import domains.rating.table.rating.RatingTable
import domains.user.objects.Username
import org.http4s.Method
import shared.api.ApiPath

import java.sql.Connection

object GetUserRating extends InternalOnlyApi[Username, RatingValue]:

  override val method: Method = Method.POST
  override val path: ApiPath = ApiPath("/api/internal/ratings/user")

  override def plan(connection: Connection, username: Username): IO[RatingValue] =
    RatingTable.findUserRating(connection, username)
