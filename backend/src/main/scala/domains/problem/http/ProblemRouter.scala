package domains.problem.http



import cats.effect.IO
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
import domains.auth.utils.SessionStore
import domains.problem.utils.ProblemDataStorage
import domains.auth.http.{ApiObjectContext, ApiObjectRouter, SessionResolver}
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl

object ProblemRouter:

  def routes(databaseSession: DatabaseSession, sessionStore: SessionStore, problemDataStorage: ProblemDataStorage): HttpRoutes[IO] =
    given Http4sDsl[IO] = new Http4sDsl[IO] {}
    val apiObjectContext = ApiObjectContext(databaseSession, SessionResolver(sessionStore))

    ApiObjectRouter.routes(
      apiObjectContext,
      List(
        ListProblemSuggestions,
        ListProblems,
        CreateProblem,
        GetProblem,
        UpdateProblem,
        DeleteProblem,
        ListProblemDataFiles(problemDataStorage),
        ListProblemDataTree,
        DownloadProblemDataPath(problemDataStorage),
        DownloadProblemData(problemDataStorage),
        DeleteProblemDataPath(problemDataStorage),
        DeleteProblemData(problemDataStorage),
        ClearProblemData(problemDataStorage),
        SetProblemDataReady(problemDataStorage),
        UploadProblemDataFile(problemDataStorage),
        UploadProblemDataArchive(problemDataStorage)
      )
    )
