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
    val handlers = new AuthenticatedHttpExecutor(databaseSession, sessionStore)

    ListProblemSets.routes(handlers) <+>
      GetProblemSet.routes(handlers) <+>
      CreateProblemSet.routes(handlers) <+>
      AddProblemToProblemSet.routes(handlers) <+>
      UpdateProblemSet.routes(handlers) <+>
      DeleteProblemSet.routes(handlers) <+>
      RemoveProblemFromProblemSet.routes(handlers)
