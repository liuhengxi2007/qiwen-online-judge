import cats.effect.{IO, IOApp}
import cats.effect.kernel.Ref
import judger.application.JudgerService
import judger.config.AppConfig
import judger.http.{JudgeHttpClient, LeaseExpiredException}
import judger.infra.ProblemDataCache
import judger.objects.RegisteredJudger
import org.typelevel.log4cats.slf4j.Slf4jLogger

import scala.concurrent.duration.DurationLong

/** judger 进程入口，负责读取配置、注册 worker、启动心跳和任务轮询服务。 */
object Main extends IOApp.Simple:
  private val logger = Slf4jLogger.getLogger[IO]

  /** 初始化外部依赖并运行长期服务；副作用包括 HTTP 注册、心跳和本地缓存目录访问。 */
  override def run: IO[Unit] =
    AppConfig.fromEnvironment(sys.env) match
      case Left(message) =>
        IO.raiseError(IllegalArgumentException(message))
      case Right(config) =>
        val httpClient = JudgeHttpClient.create(config)
        val problemDataCache = ProblemDataCache(config, httpClient)
        for
          registeredJudger <- registerJudger(httpClient)
          registeredJudgerRef <- Ref.of[IO, RegisteredJudger](registeredJudger)
          service = JudgerService(config, registeredJudgerRef, httpClient, problemDataCache, logger)
          _ <- logger.info(
            s"Starting judger ${registeredJudger.judgerId.value} against ${config.backendBaseUrl} " +
              s"(prefix=${config.preferredJudgerPrefix.value}, host=${config.host})"
          )
          heartbeatFiber <- heartbeatLoop(httpClient, registeredJudgerRef).start
          result <- service.runForever.guarantee(heartbeatFiber.cancel)
        yield result

  /** 按 backend 返回的租约心跳间隔持续上报存活状态。 */
  private def heartbeatLoop(httpClient: JudgeHttpClient, registeredJudgerRef: Ref[IO, RegisteredJudger]): IO[Nothing] =
    heartbeatIteration(httpClient, registeredJudgerRef).foreverM

  /** 执行一次心跳；当 backend 返回租约过期时会重新注册并更新共享 judger 状态。 */
  private def heartbeatIteration(httpClient: JudgeHttpClient, registeredJudgerRef: Ref[IO, RegisteredJudger]): IO[Unit] =
    registeredJudgerRef.get.flatMap { registeredJudger =>
      httpClient
        .heartbeat(registeredJudger.judgerId)
        .handleErrorWith {
          case LeaseExpiredException(_) =>
            for
              _ <- logger.warn(s"Judger lease expired for ${registeredJudger.judgerId.value}; re-registering.")
              renewedJudger <- registerJudger(httpClient)
              _ <- registeredJudgerRef.set(renewedJudger)
              _ <- logger.info(s"Re-registered judger as ${renewedJudger.judgerId.value}.")
            yield ()
          case error =>
            logger.error(error)(s"Heartbeat failed for ${registeredJudger.judgerId.value}.")
        } *> IO.sleep(registeredJudger.heartbeatIntervalMs.millis)
    }

  /** 调用注册接口获取当前进程可用的 judger 租约。 */
  private def registerJudger(httpClient: JudgeHttpClient): IO[RegisteredJudger] =
    httpClient.registerJudger.map(RegisteredJudger.fromResponse)
