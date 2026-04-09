package judger.config

import judgeprotocol.model.JudgerName

import scala.util.Try

final case class AppConfig(
  backendBaseUrl: String,
  judgeToken: String,
  judgerName: JudgerName,
  pollIntervalMs: Long,
  cxx: String
)

object AppConfig:
  def fromEnvironment(env: scala.collection.Map[String, String]): Either[String, AppConfig] =
    for
      judgerName <- JudgerName.parse(env.get("JUDGER_NAME").map(_.trim).filter(_.nonEmpty).getOrElse("cpp17-judger"))
      pollIntervalMs <- parsePositiveLong(
        env.get("POLL_INTERVAL_MS").map(_.trim).filter(_.nonEmpty).getOrElse("2000"),
        "POLL_INTERVAL_MS"
      )
    yield AppConfig(
      backendBaseUrl = env.get("BACKEND_BASE_URL").map(_.trim).filter(_.nonEmpty).getOrElse("http://localhost:8080"),
      judgeToken = env.get("JUDGE_TOKEN").map(_.trim).filter(_.nonEmpty).getOrElse("dev-judge-token"),
      judgerName = judgerName,
      pollIntervalMs = pollIntervalMs,
      cxx = env.get("CXX").map(_.trim).filter(_.nonEmpty).getOrElse("g++")
    )

  private def parsePositiveLong(raw: String, name: String): Either[String, Long] =
    Try(raw.toLong).toEither.left.map(_ => s"$name must be a valid integer.").flatMap { value =>
      if value < 1 then Left(s"$name must be greater than 0.")
      else Right(value)
    }
