package domains.system.http

import domains.auth.application.SessionStore
import cats.effect.IO
import cats.syntax.semigroupk.*
import database.DatabaseSession
import domains.judge.application.JudgeConfig
import domains.message.application.MessageEventHub
import domains.notification.application.NotificationEventHub
import domains.problem.application.ProblemDataStorage
import org.http4s.HttpApp
import org.http4s.HttpRoutes
import org.http4s.implicits.*

object ApiRouter:

  def httpApp(
    databaseSession: DatabaseSession,
    sessionStore: SessionStore,
    judgeConfig: JudgeConfig,
    problemDataStorage: ProblemDataStorage,
    messageEventHub: MessageEventHub,
    notificationEventHub: NotificationEventHub
  ): HttpApp[IO] =
    val allRoutes: HttpRoutes[IO] =
      domains.system.health.HealthRouter.routes <+>
        domains.auth.http.AuthRouter.routes(databaseSession, sessionStore) <+>
        domains.user.http.UserRouter.routes(databaseSession, sessionStore) <+>
        domains.judger.http.JudgerRegistryRouter.routes(databaseSession, judgeConfig, sessionStore) <+>
        domains.judge.http.JudgeRouter.routes(databaseSession, judgeConfig, problemDataStorage) <+>
        domains.problem.http.ProblemRouter.routes(databaseSession, sessionStore, problemDataStorage) <+>
        domains.problemset.http.ProblemSetRouter.routes(databaseSession, sessionStore) <+>
        domains.submission.http.SubmissionRouter.routes(databaseSession, sessionStore) <+>
        domains.blog.http.BlogRouter.routes(databaseSession, sessionStore, notificationEventHub) <+>
        domains.usergroup.http.UserGroupRouter.routes(databaseSession, sessionStore) <+>
        domains.message.http.MessageRouter.routes(databaseSession, sessionStore, messageEventHub) <+>
        domains.notification.http.NotificationRouter.routes(databaseSession, sessionStore, notificationEventHub)

    allRoutes.orNotFound
