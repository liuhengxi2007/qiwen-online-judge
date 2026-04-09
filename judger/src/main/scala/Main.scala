import cats.effect.{IO, IOApp}
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.parser.decode
import io.circe.syntax.*
import org.typelevel.log4cats.slf4j.Slf4jLogger

import java.net.URI
import java.net.http.{HttpClient, HttpRequest, HttpResponse}
import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path}
import java.time.Duration
import java.util.Base64
import java.util.concurrent.TimeUnit
import scala.concurrent.duration.DurationLong
import scala.util.Try

final case class SubmissionId(value: Long)
object SubmissionId:
  given Encoder[SubmissionId] = Encoder.encodeLong.contramap(_.value)
  given Decoder[SubmissionId] = Decoder.decodeLong.emap { value =>
    if value < 1 then Left("Submission id is invalid.") else Right(SubmissionId(value))
  }

final case class ProblemSlug(value: String)
object ProblemSlug:
  given Encoder[ProblemSlug] = Encoder.encodeString.contramap(_.value)
  given Decoder[ProblemSlug] = Decoder.decodeString.emap { value =>
    val normalized = value.trim
    if normalized.isEmpty then Left("Problem slug is required.") else Right(ProblemSlug(normalized))
  }

enum SubmissionLanguage:
  case Cpp17
  case Python3
object SubmissionLanguage:
  given Encoder[SubmissionLanguage] = Encoder.encodeString.contramap {
    case SubmissionLanguage.Cpp17 => "cpp17"
    case SubmissionLanguage.Python3 => "python3"
  }
  given Decoder[SubmissionLanguage] = Decoder.decodeString.emap {
    case "cpp17" => Right(SubmissionLanguage.Cpp17)
    case "python3" => Right(SubmissionLanguage.Python3)
    case other => Left(s"Unsupported submission language: $other")
  }

enum SubmissionStatus:
  case Queued
  case Running
  case Completed
  case Failed
object SubmissionStatus:
  given Encoder[SubmissionStatus] = Encoder.encodeString.contramap {
    case SubmissionStatus.Queued => "queued"
    case SubmissionStatus.Running => "running"
    case SubmissionStatus.Completed => "completed"
    case SubmissionStatus.Failed => "failed"
  }
  given Decoder[SubmissionStatus] = Decoder.decodeString.emap {
    case "queued" => Right(SubmissionStatus.Queued)
    case "running" => Right(SubmissionStatus.Running)
    case "completed" => Right(SubmissionStatus.Completed)
    case "failed" => Right(SubmissionStatus.Failed)
    case other => Left(s"Unsupported submission status: $other")
  }

enum SubmissionVerdict:
  case Accepted
  case WrongAnswer
  case CompileError
  case RuntimeError
  case TimeLimitExceeded
  case SystemError
object SubmissionVerdict:
  given Encoder[SubmissionVerdict] = Encoder.encodeString.contramap {
    case SubmissionVerdict.Accepted => "accepted"
    case SubmissionVerdict.WrongAnswer => "wrong_answer"
    case SubmissionVerdict.CompileError => "compile_error"
    case SubmissionVerdict.RuntimeError => "runtime_error"
    case SubmissionVerdict.TimeLimitExceeded => "time_limit_exceeded"
    case SubmissionVerdict.SystemError => "system_error"
  }
  given Decoder[SubmissionVerdict] = Decoder.decodeString.emap {
    case "accepted" => Right(SubmissionVerdict.Accepted)
    case "wrong_answer" => Right(SubmissionVerdict.WrongAnswer)
    case "compile_error" => Right(SubmissionVerdict.CompileError)
    case "runtime_error" => Right(SubmissionVerdict.RuntimeError)
    case "time_limit_exceeded" => Right(SubmissionVerdict.TimeLimitExceeded)
    case "system_error" => Right(SubmissionVerdict.SystemError)
    case other => Left(s"Unsupported submission verdict: $other")
  }

final case class ClaimJudgeTaskRequest(judgerName: String)
object ClaimJudgeTaskRequest:
  given Encoder[ClaimJudgeTaskRequest] = deriveEncoder[ClaimJudgeTaskRequest]

final case class JudgeTaskTestcase(
  name: String,
  inputBase64: String,
  expectedOutputBase64: String
)
object JudgeTaskTestcase:
  given Decoder[JudgeTaskTestcase] = deriveDecoder[JudgeTaskTestcase]

final case class JudgeTask(
  submissionId: SubmissionId,
  problemSlug: ProblemSlug,
  language: SubmissionLanguage,
  sourceCode: String,
  timeLimitMs: Int,
  spaceLimitMb: Int,
  testcases: List[JudgeTaskTestcase]
)
object JudgeTask:
  given Decoder[JudgeTask] = deriveDecoder[JudgeTask]

final case class ReportJudgeResultRequest(
  status: SubmissionStatus,
  verdict: Option[SubmissionVerdict],
  judgeMessage: Option[String]
)
object ReportJudgeResultRequest:
  given Encoder[ReportJudgeResultRequest] = deriveEncoder[ReportJudgeResultRequest]

final case class AppConfig(
  backendBaseUrl: String,
  judgeToken: String,
  judgerName: String,
  pollIntervalMs: Long,
  cxx: String
)

final case class ProcessResult(
  exitCode: Option[Int],
  stdout: String,
  stderr: String,
  timedOut: Boolean
)

object Main extends IOApp.Simple:
  private val logger = Slf4jLogger.getLogger[IO]

  private val config = AppConfig(
    backendBaseUrl = sys.env.get("BACKEND_BASE_URL").map(_.trim).filter(_.nonEmpty).getOrElse("http://localhost:8080"),
    judgeToken = sys.env.get("JUDGE_TOKEN").map(_.trim).filter(_.nonEmpty).getOrElse("dev-judge-token"),
    judgerName = sys.env.get("JUDGER_NAME").map(_.trim).filter(_.nonEmpty).getOrElse("cpp17-judger"),
    pollIntervalMs = sys.env.get("POLL_INTERVAL_MS").flatMap(value => Try(value.toLong).toOption).getOrElse(2000L),
    cxx = sys.env.get("CXX").map(_.trim).filter(_.nonEmpty).getOrElse("g++")
  )

  private val httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build()

  override def run: IO[Unit] =
    logger.info(s"Starting judger ${config.judgerName} against ${config.backendBaseUrl}") *>
      loop

  private def loop: IO[Nothing] =
    iteration.foreverM

  private def iteration: IO[Unit] =
    processOnce.handleErrorWith { error =>
      logger.error(error)(s"[judger] ${error.getMessage}")
    } *> IO.sleep(config.pollIntervalMs.millis)

  private def processOnce: IO[Unit] =
    claimTask.flatMap {
      case None =>
        IO.unit
      case Some(task) =>
        logger.info(s"[judger] Claimed submission #${task.submissionId.value} (${renderLanguage(task.language)}) for problem ${task.problemSlug.value}.") *>
          handleTask(task)
    }

  private def handleTask(task: JudgeTask): IO[Unit] =
    val resultIo =
      task.language match
        case SubmissionLanguage.Cpp17 => judgeCpp17Task(task)
        case other =>
          IO.pure(
            ReportJudgeResultRequest(
              status = SubmissionStatus.Failed,
              verdict = Some(SubmissionVerdict.SystemError),
              judgeMessage = Some(s"Unsupported language on this judger: ${renderLanguage(other)}.")
            )
          )

    resultIo.flatMap { result =>
      reportResult(task.submissionId, result) *>
        logger.info(
          s"[judger] Finished submission #${task.submissionId.value} with status=${renderStatus(result.status)}, verdict=${result.verdict.map(renderVerdict).getOrElse("pending")}."
        )
    }

  private def claimTask: IO[Option[JudgeTask]] =
    val body = ClaimJudgeTaskRequest(config.judgerName).asJson.noSpaces
    requestRaw(
      path = "/api/internal/judge/claim",
      method = "POST",
      body = Some(body)
    ).flatMap { response =>
      if response.statusCode == 204 then IO.pure(None)
      else if response.statusCode / 100 != 2 then
        IO.raiseError(RuntimeException(s"Claim failed with HTTP ${response.statusCode}: ${response.body}"))
      else
        IO.fromEither(decode[JudgeTask](response.body).left.map(error => RuntimeException(error.getMessage))).map(Some(_))
    }

  private def reportResult(submissionId: SubmissionId, result: ReportJudgeResultRequest): IO[Unit] =
    requestExpectSuccess(
      path = s"/api/internal/judge/submissions/${submissionId.value}/complete",
      method = "POST",
      body = Some(result.asJson.noSpaces)
    ).void

  private def requestExpectSuccess(path: String, method: String, body: Option[String]): IO[String] =
    requestRaw(path, method, body).flatMap { response =>
      if response.statusCode / 100 == 2 then IO.pure(response.body)
      else IO.raiseError(RuntimeException(s"Request failed with HTTP ${response.statusCode}: ${response.body}"))
    }

  private def requestRaw(path: String, method: String, body: Option[String]): IO[HttpResponse[String]] =
    IO.blocking {
      val builder = HttpRequest
        .newBuilder(URI.create(s"${config.backendBaseUrl}$path"))
        .timeout(Duration.ofSeconds(30))
        .header("Content-Type", "application/json")
        .header("x-judge-token", config.judgeToken)

      val request =
        body match
          case Some(value) => builder.method(method, HttpRequest.BodyPublishers.ofString(value, StandardCharsets.UTF_8)).build()
          case None => builder.method(method, HttpRequest.BodyPublishers.noBody()).build()

      httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
    }

  private def judgeCpp17Task(task: JudgeTask): IO[ReportJudgeResultRequest] =
    withWorkingDirectory("qiwen-judger-") { workingDirectory =>
      val sourceFile = workingDirectory.resolve("main.cpp")
      val executableName = if isWindows then "main.exe" else "main"
      val executablePath = workingDirectory.resolve(executableName)

      for
        _ <- IO.blocking(Files.writeString(sourceFile, task.sourceCode, StandardCharsets.UTF_8))
        compileResult <- runProcess(
          command = config.cxx,
          args = List("main.cpp", "-o", "main", "-O2", "-std=c++17"),
          cwd = workingDirectory,
          stdin = None,
          timeoutMs = math.max(task.timeLimitMs.toLong * 5, 10000L)
        )
        result <-
          if compileResult.timedOut then
            IO.pure(
              ReportJudgeResultRequest(
                status = SubmissionStatus.Failed,
                verdict = Some(SubmissionVerdict.SystemError),
                judgeMessage = Some("Compilation timed out on the judger machine.")
              )
            )
          else if compileResult.exitCode.getOrElse(-1) != 0 then
            IO.pure(
              ReportJudgeResultRequest(
                status = SubmissionStatus.Completed,
                verdict = Some(SubmissionVerdict.CompileError),
                judgeMessage = Some(nonEmptyOrFallback(compileResult.stderr, compileResult.stdout, "Compilation failed."))
              )
            )
          else
            judgeTestcases(task, workingDirectory, executablePath)
      yield result
    }.handleError { error =>
      ReportJudgeResultRequest(
        status = SubmissionStatus.Failed,
        verdict = Some(SubmissionVerdict.SystemError),
        judgeMessage = Some(error.getMessage)
      )
    }

  private def judgeTestcases(task: JudgeTask, workingDirectory: Path, executablePath: Path): IO[ReportJudgeResultRequest] =
    task.testcases.foldLeft(IO.pure(Option.empty[ReportJudgeResultRequest])) { (accIo, testcase) =>
      accIo.flatMap {
        case some @ Some(_) => IO.pure(some)
        case None =>
          val input = Base64.getDecoder.decode(testcase.inputBase64)
          val expectedOutput = new String(Base64.getDecoder.decode(testcase.expectedOutputBase64), StandardCharsets.UTF_8)
          runProcess(
            command = executablePath.toAbsolutePath.toString,
            args = Nil,
            cwd = workingDirectory,
            stdin = Some(input),
            timeoutMs = math.max(task.timeLimitMs.toLong, 1L)
          ).map { runResult =>
            if runResult.timedOut then
              Some(
                ReportJudgeResultRequest(
                  status = SubmissionStatus.Completed,
                  verdict = Some(SubmissionVerdict.TimeLimitExceeded),
                  judgeMessage = Some(s"Time limit exceeded on testcase ${testcase.name}.")
                )
              )
            else if runResult.exitCode.getOrElse(-1) != 0 then
              Some(
                ReportJudgeResultRequest(
                  status = SubmissionStatus.Completed,
                  verdict = Some(SubmissionVerdict.RuntimeError),
                  judgeMessage = Some(nonEmptyOrFallback(runResult.stderr, "", s"Runtime error on testcase ${testcase.name}."))
                )
              )
            else if normalizeOutput(runResult.stdout) != normalizeOutput(expectedOutput) then
              Some(
                ReportJudgeResultRequest(
                  status = SubmissionStatus.Completed,
                  verdict = Some(SubmissionVerdict.WrongAnswer),
                  judgeMessage = Some(s"Wrong answer on testcase ${testcase.name}.")
                )
              )
            else None
          }
      }
    }.map(
      _.getOrElse(
        ReportJudgeResultRequest(
          status = SubmissionStatus.Completed,
          verdict = Some(SubmissionVerdict.Accepted),
          judgeMessage = Some(s"Accepted by ${config.judgerName}.")
        )
      )
    )

  private def withWorkingDirectory[A](prefix: String)(use: Path => IO[A]): IO[A] =
    IO.blocking(Files.createTempDirectory(prefix)).bracket(use) { path =>
      deleteRecursively(path)
    }

  private def deleteRecursively(path: Path): IO[Unit] =
    IO.blocking {
      if Files.exists(path) then
        val paths = Files.walk(path)
        try paths.sorted(java.util.Comparator.reverseOrder()).forEach(currentPath => Files.deleteIfExists(currentPath))
        finally paths.close()
    }.void.handleError(_ => ())

  private def runProcess(
    command: String,
    args: List[String],
    cwd: Path,
    stdin: Option[Array[Byte]],
    timeoutMs: Long
  ): IO[ProcessResult] =
    IO.blocking {
      val builder = new ProcessBuilder((command :: args)*)
      builder.directory(cwd.toFile)
      val process = builder.start()

      stdin.foreach { bytes =>
        val stream = process.getOutputStream
        try stream.write(bytes)
        finally stream.close()
      }
      if stdin.isEmpty then process.getOutputStream.close()

      val completed = process.waitFor(timeoutMs, TimeUnit.MILLISECONDS)
      if !completed then
        process.destroyForcibly()
        process.waitFor(5, TimeUnit.SECONDS)

      val stdout = new String(process.getInputStream.readAllBytes(), StandardCharsets.UTF_8)
      val stderr = new String(process.getErrorStream.readAllBytes(), StandardCharsets.UTF_8)

      ProcessResult(
        exitCode = if completed then Some(process.exitValue()) else None,
        stdout = stdout,
        stderr = stderr,
        timedOut = !completed
      )
    }

  private def normalizeOutput(value: String): String =
    value.replace("\r\n", "\n").replace('\r', '\n').stripTrailing()

  private def nonEmptyOrFallback(primary: String, secondary: String, fallback: String): String =
    val first = primary.trim
    if first.nonEmpty then first
    else
      val second = secondary.trim
      if second.nonEmpty then second else fallback

  private def renderLanguage(language: SubmissionLanguage): String =
    language match
      case SubmissionLanguage.Cpp17 => "cpp17"
      case SubmissionLanguage.Python3 => "python3"

  private def renderStatus(status: SubmissionStatus): String =
    status match
      case SubmissionStatus.Queued => "queued"
      case SubmissionStatus.Running => "running"
      case SubmissionStatus.Completed => "completed"
      case SubmissionStatus.Failed => "failed"

  private def renderVerdict(verdict: SubmissionVerdict): String =
    verdict match
      case SubmissionVerdict.Accepted => "accepted"
      case SubmissionVerdict.WrongAnswer => "wrong_answer"
      case SubmissionVerdict.CompileError => "compile_error"
      case SubmissionVerdict.RuntimeError => "runtime_error"
      case SubmissionVerdict.TimeLimitExceeded => "time_limit_exceeded"
      case SubmissionVerdict.SystemError => "system_error"

  private def isWindows: Boolean =
    sys.props.get("os.name").exists(_.toLowerCase.contains("win"))
