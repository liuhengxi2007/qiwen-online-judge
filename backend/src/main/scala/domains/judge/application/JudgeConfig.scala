package domains.judge.application

import scala.util.Try

final case class JudgeConfig(
  sharedToken: String,
  heartbeatIntervalMs: Long,
  heartbeatTimeoutMs: Long
)

object JudgeConfig:
  def loadFromEnvironment(): JudgeConfig =
    val configuredToken = sys.env.get("JUDGE_SHARED_TOKEN").map(_.trim).filter(_.nonEmpty)
    JudgeConfig(
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
