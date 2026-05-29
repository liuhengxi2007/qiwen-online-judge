package domains.problemset.routes



import cats.effect.IO
import database.DatabaseSession
import domains.problemset.api.ListProblemSets
import domains.problemset.api.GetProblemSet
import domains.problemset.api.CreateProblemSet
import domains.problemset.api.AddProblemToProblemSet
import domains.problemset.api.UpdateProblemSet
import domains.problemset.api.DeleteProblemSet
import domains.problemset.api.RemoveProblemFromProblemSet
import domains.problemset.api.ResolveProblemSetSlug
import domains.auth.utils.SessionStore
import domains.auth.api.{ApiObjectContext, ApiObjectRouter, SessionResolver}
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
        DeleteProblemSet,
        ResolveProblemSetSlug
      )
    )
