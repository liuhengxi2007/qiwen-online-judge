package domains.judge.application

final case class JudgeConfig(sharedToken: String)

object JudgeConfig:
  def loadFromEnvironment(): JudgeConfig =
    val configuredToken = sys.env.get("JUDGE_SHARED_TOKEN").map(_.trim).filter(_.nonEmpty)
    JudgeConfig(sharedToken = configuredToken.getOrElse("dev-judge-token"))
