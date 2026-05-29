package domains.problem.routes



import cats.effect.IO
import database.DatabaseSession
import domains.problem.api.ListProblems
import domains.problem.api.ListProblemSuggestions
import domains.problem.api.CreateProblem
import domains.problem.api.GetProblem
import domains.problem.api.ListProblemDataFiles
import domains.problem.api.ListProblemDataTree
import domains.problem.api.DownloadProblemDataPath
import domains.problem.api.EvaluateProblemAccess
import domains.problem.api.GetJudgeProblemDataManifest
import domains.problem.api.ResolveProblemReference
import domains.problem.api.DeleteProblemDataPath
import domains.problem.api.ClearProblemData
import domains.problem.api.SetProblemDataReady
import domains.problem.api.UploadProblemDataFile
import domains.problem.api.UploadProblemDataArchive
import domains.problem.api.UpdateProblem
import domains.problem.api.DeleteProblem
import domains.auth.utils.SessionStore
import domains.problem.utils.ProblemDataStorage
import domains.auth.api.{ApiObjectContext, ApiObjectRouter, SessionResolver}
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
        DeleteProblemDataPath(problemDataStorage),
        ClearProblemData(problemDataStorage),
        SetProblemDataReady(problemDataStorage),
        UploadProblemDataFile(problemDataStorage),
        UploadProblemDataArchive(problemDataStorage),
        ResolveProblemReference,
        EvaluateProblemAccess,
        GetJudgeProblemDataManifest
      )
    )
