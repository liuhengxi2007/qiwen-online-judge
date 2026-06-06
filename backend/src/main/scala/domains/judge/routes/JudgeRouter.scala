package domains.judge.routes

import cats.effect.IO
import database.DatabaseSession
import domains.auth.api.{ApiObjectContext, ApiObjectRouter}
import domains.judge.utils.JudgeConfig
import domains.judge.api.*
import domains.problem.utils.ProblemDataStorage
import domains.submission.utils.SubmissionProgramStorage
import org.http4s.HttpRoutes

object JudgeRouter:

  def routes(
    databaseSession: DatabaseSession,
    judgeConfig: JudgeConfig,
    problemDataStorage: ProblemDataStorage,
    submissionProgramStorage: SubmissionProgramStorage
  ): HttpRoutes[IO] =
    ApiObjectRouter.routes(
      ApiObjectContext.public(databaseSession),
      List(
        ClaimJudgeTask(judgeConfig, problemDataStorage, submissionProgramStorage),
        DownloadJudgeProblemData(judgeConfig, problemDataStorage),
        CompleteJudgeSubmission(judgeConfig),
        CompleteHackAttempt(judgeConfig)
      )
    )
