package domains.hack.routes

import cats.effect.IO
import database.DatabaseSession
import domains.auth.api.{ApiObjectContext, ApiObjectRouter, SessionResolver}
import domains.auth.utils.SessionStore
import domains.hack.api.*
import domains.problem.utils.ProblemDataStorage
import domains.submission.utils.SubmissionProgramStorage
import org.http4s.HttpRoutes

object HackRouter:

  def routes(
    databaseSession: DatabaseSession,
    sessionStore: SessionStore,
    submissionProgramStorage: SubmissionProgramStorage,
    problemDataStorage: ProblemDataStorage
  ): HttpRoutes[IO] =
    val context = ApiObjectContext(databaseSession, SessionResolver(sessionStore))

    ApiObjectRouter.routes(
      context,
      List(
        GetSubmissionHackSubtask(submissionProgramStorage, problemDataStorage),
        GetSubmissionHackAvailability(submissionProgramStorage, problemDataStorage),
        CreateHack(submissionProgramStorage, problemDataStorage),
        GetHack,
        ListHacks,
        ClaimNextHackAttempt,
        ListProblemHackTestcasesForJudge,
        ReadHackProblemData,
        RecordHackAttemptResult
      )
    )
