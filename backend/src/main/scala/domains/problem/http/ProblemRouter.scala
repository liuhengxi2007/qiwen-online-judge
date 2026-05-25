package domains.problem.http



import cats.effect.IO
import cats.syntax.semigroupk.*
import database.DatabaseSession
import domains.problem.http.api.ListProblems
import domains.problem.http.api.ListProblemSuggestions
import domains.problem.http.api.CreateProblem
import domains.problem.http.api.GetProblem
import domains.problem.http.api.ListProblemDataFiles
import domains.problem.http.api.ListProblemDataTree
import domains.problem.http.api.DownloadProblemDataPath
import domains.problem.http.api.DeleteProblemDataPath
import domains.problem.http.api.DownloadProblemData
import domains.problem.http.api.DeleteProblemData
import domains.problem.http.api.ClearProblemData
import domains.problem.http.api.SetProblemDataReady
import domains.problem.http.api.UploadProblemDataFile
import domains.problem.http.api.UploadProblemDataArchive
import domains.problem.http.api.UpdateProblem
import domains.problem.http.api.DeleteProblem
import domains.auth.application.SessionStore
import domains.problem.application.ProblemDataStorage
import domains.auth.http.AuthenticatedHttpExecutor
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl

final case class ProblemHttpRouteContext(
  databaseSession: DatabaseSession,
  sessionStore: SessionStore,
  problemDataStorage: ProblemDataStorage,
  handlers: AuthenticatedHttpExecutor,
  plans: ProblemHttpPlanDefinitions.RegisteredPlans
)

object ProblemRouter:

  def routes(databaseSession: DatabaseSession, sessionStore: SessionStore, problemDataStorage: ProblemDataStorage): HttpRoutes[IO] =
    given Http4sDsl[IO] = new Http4sDsl[IO] {}
    val context = ProblemHttpRouteContext(
      databaseSession = databaseSession,
      sessionStore = sessionStore,
      problemDataStorage = problemDataStorage,
      handlers = new AuthenticatedHttpExecutor(databaseSession, sessionStore),
      plans = ProblemHttpPlanDefinitions.plans(problemDataStorage)
    )

    ListProblems.routes(context) <+>
      ListProblemSuggestions.routes(context) <+>
      CreateProblem.routes(context) <+>
      GetProblem.routes(context) <+>
      ListProblemDataFiles.routes(context) <+>
      ListProblemDataTree.routes(context) <+>
      DownloadProblemDataPath.routes(context) <+>
      DeleteProblemDataPath.routes(context) <+>
      DownloadProblemData.routes(context) <+>
      DeleteProblemData.routes(context) <+>
      ClearProblemData.routes(context) <+>
      SetProblemDataReady.routes(context) <+>
      UploadProblemDataFile.routes(context) <+>
      UploadProblemDataArchive.routes(context) <+>
      UpdateProblem.routes(context) <+>
      DeleteProblem.routes(context)
