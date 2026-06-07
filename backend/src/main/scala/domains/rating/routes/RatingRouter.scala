package domains.rating.routes

import cats.effect.IO
import database.DatabaseSession
import domains.auth.api.{ApiObjectContext, ApiObjectRouter, SessionResolver}
import domains.auth.utils.SessionStore
import domains.rating.api.*
import org.http4s.HttpRoutes

object RatingRouter:

  def routes(databaseSession: DatabaseSession, sessionStore: SessionStore): HttpRoutes[IO] =
    val apiObjectContext = ApiObjectContext(databaseSession, SessionResolver(sessionStore))

    ApiObjectRouter.routes(
      apiObjectContext,
      List(
        ListRatingRanklist,
        GetRatingManageState,
        AppendRatingContest,
        PopRatingContest,
        GetUserRating
      )
    )
