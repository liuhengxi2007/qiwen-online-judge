package domains.judge.utils

import scala.util.Try

/** 判题服务配置；包含 worker 共享 token 和 judger 心跳租约参数。 */
final case class JudgeConfig(
  sharedToken: String,
  heartbeatIntervalMs: Long,
  heartbeatTimeoutMs: Long
)

/** 判题配置加载器；从环境变量读取 token 和心跳间隔，非法数值退回默认值。 */
object JudgeConfig:
  /** 从进程环境构造判题配置；未配置 token 时使用开发默认值。 */
  def loadFromEnvironment(): JudgeConfig =
    val configuredToken = sys.env.get("JUDGE_SHARED_TOKEN").map(_.trim).filter(_.nonEmpty)
    JudgeConfig(
      // FIXME-CN: 未配置 JUDGE_SHARED_TOKEN 时会启用公开的开发默认 token；生产部署如果漏配会让内部判题接口失去有效隔离。
      sharedToken = configuredToken.getOrElse("dev-judge-token"),
      heartbeatIntervalMs = parsePositiveLong("JUDGER_HEARTBEAT_INTERVAL_MS", 5000L),
      heartbeatTimeoutMs = parsePositiveLong("JUDGER_HEARTBEAT_TIMEOUT_MS", 15000L)
    )

  private def parsePositiveLong(name: String, defaultValue: Long): Long =
    sys.env
      .get(name)
      .map(_.trim)
      .filter(_.nonEmpty)
      .flatMap(raw => Try(raw.toLong).toOption)
      .filter(_ > 0)
      .getOrElse(defaultValue)
