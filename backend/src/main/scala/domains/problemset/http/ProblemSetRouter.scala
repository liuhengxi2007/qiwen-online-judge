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
import org.http4s.HttpRoutes

object ProblemSetRouter:

  def routes(databaseSession: DatabaseSession, sessionStore: SessionStore): HttpRoutes[IO] =
    ListProblemSets.routes(databaseSession, sessionStore) <+>
      GetProblemSet.routes(databaseSession, sessionStore) <+>
      CreateProblemSet.routes(databaseSession, sessionStore) <+>
      AddProblemToProblemSet.routes(databaseSession, sessionStore) <+>
      UpdateProblemSet.routes(databaseSession, sessionStore) <+>
      DeleteProblemSet.routes(databaseSession, sessionStore) <+>
      RemoveProblemFromProblemSet.routes(databaseSession, sessionStore)
