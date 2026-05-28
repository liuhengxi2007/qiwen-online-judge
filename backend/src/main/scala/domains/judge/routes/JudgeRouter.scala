package domains.judge.routes

import cats.effect.IO
import database.DatabaseSession
import domains.auth.api.{ApiObjectContext, ApiObjectRouter}
import domains.judge.utils.JudgeConfig
import domains.judge.api.*
import domains.problem.utils.ProblemDataStorage
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
