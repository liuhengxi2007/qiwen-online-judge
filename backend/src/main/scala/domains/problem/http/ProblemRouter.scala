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
import org.http4s.HttpRoutes

object ProblemRouter:

  def routes(databaseSession: DatabaseSession, sessionStore: SessionStore, problemDataStorage: ProblemDataStorage): HttpRoutes[IO] =
    ListProblems.routes(databaseSession, sessionStore, problemDataStorage) <+>
      ListProblemSuggestions.routes(databaseSession, sessionStore, problemDataStorage) <+>
      CreateProblem.routes(databaseSession, sessionStore, problemDataStorage) <+>
      GetProblem.routes(databaseSession, sessionStore, problemDataStorage) <+>
      ListProblemDataFiles.routes(databaseSession, sessionStore, problemDataStorage) <+>
      ListProblemDataTree.routes(databaseSession, sessionStore, problemDataStorage) <+>
      DownloadProblemDataPath.routes(databaseSession, sessionStore, problemDataStorage) <+>
      DeleteProblemDataPath.routes(databaseSession, sessionStore, problemDataStorage) <+>
      DownloadProblemData.routes(databaseSession, sessionStore, problemDataStorage) <+>
      DeleteProblemData.routes(databaseSession, sessionStore, problemDataStorage) <+>
      ClearProblemData.routes(databaseSession, sessionStore, problemDataStorage) <+>
      SetProblemDataReady.routes(databaseSession, sessionStore, problemDataStorage) <+>
      UploadProblemDataFile.routes(databaseSession, sessionStore, problemDataStorage) <+>
      UploadProblemDataArchive.routes(databaseSession, sessionStore, problemDataStorage) <+>
      UpdateProblem.routes(databaseSession, sessionStore, problemDataStorage) <+>
      DeleteProblem.routes(databaseSession, sessionStore, problemDataStorage)
