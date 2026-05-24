package domains.judge.http

import cats.effect.IO
import cats.syntax.semigroupk.*
import database.DatabaseSession
import domains.judge.application.JudgeConfig
import domains.judge.http.api.ClaimJudgeTask
import domains.judge.http.api.DownloadJudgeProblemData
import domains.judge.http.api.CompleteJudgeSubmission
import domains.problem.application.ProblemDataStorage
import org.http4s.HttpRoutes

object JudgeRouter:

  def routes(databaseSession: DatabaseSession, judgeConfig: JudgeConfig, problemDataStorage: ProblemDataStorage): HttpRoutes[IO] =
    ClaimJudgeTask.routes(databaseSession, judgeConfig, problemDataStorage) <+>
      DownloadJudgeProblemData.routes(databaseSession, judgeConfig, problemDataStorage) <+>
      CompleteJudgeSubmission.routes(databaseSession, judgeConfig, problemDataStorage)
