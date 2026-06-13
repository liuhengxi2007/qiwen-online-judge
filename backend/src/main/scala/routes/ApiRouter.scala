package routes

import cats.effect.IO
import cats.syntax.semigroupk.*
import database.DatabaseSession
import domains.auth.utils.SessionStore
import domains.judge.utils.JudgeConfig
import domains.message.utils.MessageEventHub
import domains.notification.utils.NotificationEventHub
import domains.problem.utils.ProblemDataStorage
import domains.submission.utils.SubmissionProgramStorage
import domains.user.utils.UserAvatarStorage
import org.http4s.HttpApp
import org.http4s.HttpRoutes
import org.http4s.implicits.*

/** 全站 API 路由聚合器，负责把各领域路由组合成最终 HttpApp。 */
object ApiRouter:

  /** 注入跨领域运行时依赖并按领域顺序组合所有后端 API 路由。 */
  def httpApp(
    databaseSession: DatabaseSession,
    sessionStore: SessionStore,
    judgeConfig: JudgeConfig,
    problemDataStorage: ProblemDataStorage,
    submissionProgramStorage: SubmissionProgramStorage,
    userAvatarStorage: UserAvatarStorage,
    messageEventHub: MessageEventHub,
    notificationEventHub: NotificationEventHub
  ): HttpApp[IO] =
    val allRoutes: HttpRoutes[IO] =
      domains.auth.routes.AuthRouter.routes(databaseSession, sessionStore) <+>
        domains.user.routes.UserRouter.routes(databaseSession, sessionStore, userAvatarStorage) <+>
        domains.judger.routes.JudgerRegistryRouter.routes(databaseSession, judgeConfig, sessionStore) <+>
        domains.judge.routes.JudgeRouter.routes(databaseSession, judgeConfig, problemDataStorage, submissionProgramStorage) <+>
        domains.problem.routes.ProblemRouter.routes(databaseSession, sessionStore, problemDataStorage, submissionProgramStorage) <+>
        domains.problemset.routes.ProblemSetRouter.routes(databaseSession, sessionStore) <+>
        domains.contest.routes.ContestRouter.routes(databaseSession, sessionStore, submissionProgramStorage, problemDataStorage) <+>
        domains.submission.routes.SubmissionRouter.routes(databaseSession, sessionStore, submissionProgramStorage, problemDataStorage) <+>
        domains.hack.routes.HackRouter.routes(databaseSession, sessionStore, submissionProgramStorage, problemDataStorage) <+>
        domains.blog.routes.BlogRouter.routes(databaseSession, sessionStore, notificationEventHub) <+>
        domains.usergroup.routes.UserGroupRouter.routes(databaseSession, sessionStore) <+>
        domains.message.routes.MessageRouter.routes(databaseSession, sessionStore, messageEventHub) <+>
        domains.notification.routes.NotificationRouter.routes(databaseSession, sessionStore, notificationEventHub) <+>
        domains.rating.routes.RatingRouter.routes(databaseSession, sessionStore)

    allRoutes.orNotFound
