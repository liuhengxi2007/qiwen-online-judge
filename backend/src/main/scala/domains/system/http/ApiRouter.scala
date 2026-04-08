package domains.system.http

import domains.auth.application.SessionStore
import cats.effect.IO
import cats.syntax.semigroupk.*
import database.DatabaseSession
import domains.judge.application.JudgeConfig
import org.http4s.HttpApp
import org.http4s.HttpRoutes
import org.http4s.implicits.*

object ApiRouter:

  def httpApp(databaseSession: DatabaseSession, sessionStore: SessionStore, judgeConfig: JudgeConfig): HttpApp[IO] =
    val allRoutes: HttpRoutes[IO] =
      domains.system.health.HealthRouter.routes <+>
        domains.auth.http.AuthRouter.routes(databaseSession, sessionStore) <+>
        domains.judge.http.JudgeRouter.routes(databaseSession, judgeConfig) <+>
        domains.problem.http.ProblemRouter.routes(databaseSession, sessionStore) <+>
        domains.problemset.http.ProblemSetRouter.routes(databaseSession, sessionStore) <+>
        domains.submission.http.SubmissionRouter.routes(databaseSession, sessionStore) <+>
        domains.usergroup.http.UserGroupRouter.routes(databaseSession, sessionStore)

    allRoutes.orNotFound
