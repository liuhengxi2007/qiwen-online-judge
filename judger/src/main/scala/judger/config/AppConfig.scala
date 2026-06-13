package judger.config

import judgeprotocol.objects.{JudgerId, SubmissionLanguage}

import java.net.InetAddress
import java.nio.file.Path
import scala.util.Try

/** judger 运行所需的环境配置，覆盖 backend 地址、鉴权 token、编译器、sandbox 和缓存路径。 */
final case class AppConfig(
  backendBaseUrl: String,
  judgeToken: String,
  preferredJudgerPrefix: JudgerId,
  host: String,
  processId: Option[String],
  supportedLanguages: List[SubmissionLanguage],
  pollIntervalMs: Long,
  cxx: String,
  python3: String,
  isolateBin: String,
  isolateBoxId: Int,
  preferIsolateCgroups: Boolean,
  workRoot: Path,
  problemDataCacheRoot: Path
)

/** 从环境变量构造 AppConfig，并为本地开发提供默认值。 */
object AppConfig:
  /** 读取并校验环境变量；返回错误字符串而不是抛异常，入口层负责终止进程。 */
  def fromEnvironment(env: scala.collection.Map[String, String]): Either[String, AppConfig] =
    for
      preferredJudgerPrefix <- JudgerId.parse(
        env.get("JUDGER_ID_PREFIX").map(_.trim).filter(_.nonEmpty).getOrElse("local-judger")
      )
      judgeToken <- requiredString(env, "JUDGE_TOKEN")
      pollIntervalMs <- parsePositiveLong(
        env.get("POLL_INTERVAL_MS").map(_.trim).filter(_.nonEmpty).getOrElse("2000"),
        "POLL_INTERVAL_MS"
      )
    yield AppConfig(
      backendBaseUrl = env.get("BACKEND_BASE_URL").map(_.trim).filter(_.nonEmpty).getOrElse("http://localhost:8080"),
      judgeToken = judgeToken,
      preferredJudgerPrefix = preferredJudgerPrefix,
      host = env.get("JUDGER_HOST").map(_.trim).filter(_.nonEmpty).getOrElse(detectHost()),
      processId = env.get("JUDGER_PROCESS_ID").map(_.trim).filter(_.nonEmpty).orElse(detectProcessId()),
      supportedLanguages = List(SubmissionLanguage.Cpp17, SubmissionLanguage.Python3),
      pollIntervalMs = pollIntervalMs,
      cxx = env.get("CXX").map(_.trim).filter(_.nonEmpty).getOrElse("g++"),
      python3 = env.get("PYTHON3").map(_.trim).filter(_.nonEmpty).getOrElse("python3"),
      isolateBin = env.get("ISOLATE_BIN").map(_.trim).filter(_.nonEmpty).getOrElse("isolate"),
      // 注意：未显式配置 isolate box id 时按进程号派生，降低同机多个本地 worker 的默认冲突概率。
      isolateBoxId = parseBoxId(env.get("ISOLATE_BOX_ID"), env.get("JUDGER_PROCESS_ID").orElse(detectProcessId())),
      preferIsolateCgroups = parseBoolean(env.get("ISOLATE_PREFER_CGROUPS"), defaultValue = true),
      workRoot = Path.of(env.get("JUDGER_WORK_ROOT").map(_.trim).filter(_.nonEmpty).getOrElse(defaultWorkRoot().toString)),
      problemDataCacheRoot = Path.of(
        env.get("JUDGER_PROBLEM_DATA_CACHE_ROOT").map(_.trim).filter(_.nonEmpty).getOrElse(defaultProblemDataCacheRoot().toString)
      )
    )

  private def requiredString(env: scala.collection.Map[String, String], name: String): Either[String, String] =
    env.get(name).map(_.trim).filter(_.nonEmpty).toRight(s"$name must be configured.")

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

  private def defaultProblemDataCacheRoot(): Path =
    defaultWorkRoot().resolve("problem-data-cache")
