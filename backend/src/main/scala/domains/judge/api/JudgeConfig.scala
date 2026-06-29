package domains.judge.api

import scala.util.Try

/** 判题服务配置；包含 worker 共享 token 和 judger 心跳租约参数。 */
final case class JudgeConfig(
  sharedToken: String,
  heartbeatIntervalMs: Long,
  heartbeatTimeoutMs: Long
)

/** 判题配置加载器；从环境变量读取 token 和心跳间隔，非法数值退回默认值。 */
object JudgeConfig:
  /** 从进程环境构造判题配置。 */
  def loadFromEnvironment(): JudgeConfig =
    JudgeConfig(
      sharedToken = requiredString("JUDGE_SHARED_TOKEN"),
      heartbeatIntervalMs = parsePositiveLong("JUDGER_HEARTBEAT_INTERVAL_MS", 5000L),
      heartbeatTimeoutMs = parsePositiveLong("JUDGER_HEARTBEAT_TIMEOUT_MS", 15000L)
    )

  private def requiredString(name: String): String =
    sys.env.get(name).map(_.trim).filter(_.nonEmpty).getOrElse {
      throw IllegalStateException(s"$name must be configured.")
    }

  private def parsePositiveLong(name: String, defaultValue: Long): Long =
    sys.env
      .get(name)
      .map(_.trim)
      .filter(_.nonEmpty)
      .flatMap(raw => Try(raw.toLong).toOption)
      .filter(_ > 0)
      .getOrElse(defaultValue)
