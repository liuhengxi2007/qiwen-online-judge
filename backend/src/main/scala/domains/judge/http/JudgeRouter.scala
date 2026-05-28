package domains.judge.http

import cats.effect.IO
import database.DatabaseSession
import domains.auth.http.{ApiObjectContext, ApiObjectRouter}
import domains.judge.application.JudgeConfig
import domains.judge.http.api.*
import domains.problem.application.ProblemDataStorage
import org.http4s.HttpRoutes

object JudgeRouter:

  def routes(databaseSession: DatabaseSession, judgeConfig: JudgeConfig, problemDataStorage: ProblemDataStorage): HttpRoutes[IO] =
    ApiObjectRouter.routes(
      ApiObjectContext.public(databaseSession),
      List(
        ClaimJudgeTask(judgeConfig, problemDataStorage),
        DownloadJudgeProblemData(judgeConfig, problemDataStorage),
        CompleteJudgeSubmission(judgeConfig)
      )
    )
