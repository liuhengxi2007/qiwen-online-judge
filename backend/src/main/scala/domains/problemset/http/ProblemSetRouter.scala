package domains.problemset.http



import cats.effect.IO
import database.DatabaseSession
import domains.problemset.http.api.ListProblemSets
import domains.problemset.http.api.GetProblemSet
import domains.problemset.http.api.CreateProblemSet
import domains.problemset.http.api.AddProblemToProblemSet
import domains.problemset.http.api.UpdateProblemSet
import domains.problemset.http.api.DeleteProblemSet
import domains.problemset.http.api.RemoveProblemFromProblemSet
import domains.auth.application.SessionStore
import domains.auth.http.{ApiObjectContext, ApiObjectRouter, SessionResolver}
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl

object ProblemSetRouter:

  def routes(databaseSession: DatabaseSession, sessionStore: SessionStore): HttpRoutes[IO] =
    given Http4sDsl[IO] = new Http4sDsl[IO] {}
    val apiObjectContext = ApiObjectContext(databaseSession, SessionResolver(sessionStore))

    ApiObjectRouter.routes(
      apiObjectContext,
      List(
        ListProblemSets,
        CreateProblemSet,
        AddProblemToProblemSet,
        RemoveProblemFromProblemSet,
        GetProblemSet,
        UpdateProblemSet,
        DeleteProblemSet
      )
    )
