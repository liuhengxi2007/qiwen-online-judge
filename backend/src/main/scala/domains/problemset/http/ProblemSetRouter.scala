package domains.problemset.http



import cats.effect.IO
import cats.syntax.semigroupk.*
import database.DatabaseSession
import domains.problemset.http.api.ListProblemSets
import domains.problemset.http.api.GetProblemSet
import domains.problemset.http.api.CreateProblemSet
import domains.problemset.http.api.AddProblemToProblemSet
import domains.problemset.http.api.UpdateProblemSet
import domains.problemset.http.api.DeleteProblemSet
import domains.problemset.http.api.RemoveProblemFromProblemSet
import domains.auth.application.SessionStore
import domains.auth.http.AuthenticatedHttpExecutor
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl

object ProblemSetRouter:

  def routes(databaseSession: DatabaseSession, sessionStore: SessionStore): HttpRoutes[IO] =
    given Http4sDsl[IO] = new Http4sDsl[IO] {}
    val context = ProblemSetHttpRouteContext(
      databaseSession = databaseSession,
      sessionStore = sessionStore,
      handlers = new AuthenticatedHttpExecutor(databaseSession, sessionStore)
    )

    ListProblemSets.routes(context) <+>
      GetProblemSet.routes(context) <+>
      CreateProblemSet.routes(context) <+>
      AddProblemToProblemSet.routes(context) <+>
      UpdateProblemSet.routes(context) <+>
      DeleteProblemSet.routes(context) <+>
      RemoveProblemFromProblemSet.routes(context)
