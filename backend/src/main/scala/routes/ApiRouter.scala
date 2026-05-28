package routes

import cats.effect.IO
import cats.syntax.semigroupk.*
import database.DatabaseSession
import domains.auth.utils.SessionStore
import domains.judge.utils.JudgeConfig
import domains.message.utils.MessageEventHub
import domains.notification.utils.NotificationEventHub
import domains.problem.utils.ProblemDataStorage
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
      routes.HealthRouter.routes <+>
        domains.auth.routes.AuthRouter.routes(databaseSession, sessionStore) <+>
        domains.user.routes.UserRouter.routes(databaseSession, sessionStore) <+>
        domains.judger.routes.JudgerRegistryRouter.routes(databaseSession, judgeConfig, sessionStore) <+>
        domains.judge.routes.JudgeRouter.routes(databaseSession, judgeConfig, problemDataStorage) <+>
        domains.problem.routes.ProblemRouter.routes(databaseSession, sessionStore, problemDataStorage) <+>
        domains.problemset.routes.ProblemSetRouter.routes(databaseSession, sessionStore) <+>
        domains.submission.routes.SubmissionRouter.routes(databaseSession, sessionStore) <+>
        domains.blog.routes.BlogRouter.routes(databaseSession, sessionStore, notificationEventHub) <+>
        domains.usergroup.routes.UserGroupRouter.routes(databaseSession, sessionStore) <+>
        domains.message.routes.MessageRouter.routes(databaseSession, sessionStore, messageEventHub) <+>
        domains.notification.routes.NotificationRouter.routes(databaseSession, sessionStore, notificationEventHub)

    allRoutes.orNotFound
