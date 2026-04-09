package judger.config

import judgeprotocol.model.{JudgerId, SubmissionLanguage}

import java.net.InetAddress
import java.nio.file.Path
import scala.util.Try

final case class AppConfig(
  backendBaseUrl: String,
  judgeToken: String,
  preferredJudgerPrefix: JudgerId,
  host: String,
  processId: Option[String],
  supportedLanguages: List[SubmissionLanguage],
  pollIntervalMs: Long,
  cxx: String,
  isolateBin: String,
  isolateBoxId: Int,
  preferIsolateCgroups: Boolean,
  workRoot: Path
)

object AppConfig:
  def fromEnvironment(env: scala.collection.Map[String, String]): Either[String, AppConfig] =
    for
      preferredJudgerPrefix <- JudgerId.parse(
        env.get("JUDGER_ID_PREFIX").map(_.trim).filter(_.nonEmpty).getOrElse("local-judger")
      )
      pollIntervalMs <- parsePositiveLong(
        env.get("POLL_INTERVAL_MS").map(_.trim).filter(_.nonEmpty).getOrElse("2000"),
        "POLL_INTERVAL_MS"
      )
    yield AppConfig(
      backendBaseUrl = env.get("BACKEND_BASE_URL").map(_.trim).filter(_.nonEmpty).getOrElse("http://localhost:8080"),
      judgeToken = env.get("JUDGE_TOKEN").map(_.trim).filter(_.nonEmpty).getOrElse("dev-judge-token"),
      preferredJudgerPrefix = preferredJudgerPrefix,
      host = env.get("JUDGER_HOST").map(_.trim).filter(_.nonEmpty).getOrElse(detectHost()),
      processId = env.get("JUDGER_PROCESS_ID").map(_.trim).filter(_.nonEmpty).orElse(detectProcessId()),
      supportedLanguages = List(SubmissionLanguage.Cpp17),
      pollIntervalMs = pollIntervalMs,
      cxx = env.get("CXX").map(_.trim).filter(_.nonEmpty).getOrElse("g++"),
      isolateBin = env.get("ISOLATE_BIN").map(_.trim).filter(_.nonEmpty).getOrElse("isolate"),
      isolateBoxId = parseBoxId(env.get("ISOLATE_BOX_ID"), env.get("JUDGER_PROCESS_ID").orElse(detectProcessId())),
      preferIsolateCgroups = parseBoolean(env.get("ISOLATE_PREFER_CGROUPS"), defaultValue = true),
      workRoot = Path.of(
        env.get("JUDGER_WORK_ROOT").map(_.trim).filter(_.nonEmpty).getOrElse(defaultWorkRoot().toString)
      )
    )

  private def parsePositiveLong(raw: String, name: String): Either[String, Long] =
    Try(raw.toLong).toEither.left.map(_ => s"$name must be a valid integer.").flatMap { value =>
      if value < 1 then Left(s"$name must be greater than 0.")
      else Right(value)
    }

  private def detectHost(): String =
    Try(InetAddress.getLocalHost.getHostName).toOption.map(_.trim).filter(_.nonEmpty).getOrElse("localhost")

  private def detectProcessId(): Option[String] =
    Try(ProcessHandle.current().pid().toString).toOption.filter(_.nonEmpty)

  private def parseBoxId(configured: Option[String], processId: Option[String]): Int =
    configured
      .map(_.trim)
      .filter(_.nonEmpty)
      .flatMap(raw => Try(raw.toInt).toOption)
      .filter(boxId => boxId >= 0 && boxId <= 999)
      .orElse {
        processId
          .flatMap(raw => Try(raw.toLong).toOption)
          .map(pid => ((pid % 900) + 100).toInt)
      }
      .getOrElse(100)

  private def parseBoolean(raw: Option[String], defaultValue: Boolean): Boolean =
    raw
      .map(_.trim.toLowerCase)
      .collect {
        case "1" | "true" | "yes" | "on" => true
        case "0" | "false" | "no" | "off" => false
      }
      .getOrElse(defaultValue)

  private def defaultWorkRoot(): Path =
    Path.of(sys.props.getOrElse("user.home", "."), ".cache", "qiwen-judger")
