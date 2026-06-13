package judger.infra

import cats.effect.IO
import judgeprotocol.objects.{SubmissionLanguage, SubmissionSourceCode, SubmissionStatus, SubmissionVerdict}
import judgeprotocol.objects.request.ReportJudgeResultRequest
import judgeprotocol.objects.response.{JudgeFailureReason, JudgeResult, JudgeResultSummary, JudgeSubtaskResult, JudgeTask}
import judger.config.AppConfig
import judger.objects.{ProcessResult, RuntimeCommand, SandboxLimits}

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path}
import java.nio.file.attribute.PosixFilePermissions
import java.util.concurrent.TimeUnit

/** 某种提交语言的准备器，将源代码编译或转换为 sandbox 可执行命令。 */
trait JudgeRuntime:
  /** 当前 runtime 支持的协议语言。 */
  def language: SubmissionLanguage

  /** 准备一个 role 的程序；输入是源码、可选 stub 和头文件，输出可执行命令或编译/系统失败。 */
  def prepare(
    role: String,
    sourceCode: SubmissionSourceCode,
    stubSourceCode: Option[SubmissionSourceCode],
    headers: List[ProgramHeaderSource],
    config: AppConfig,
    workingDirectory: Path
  ): IO[Either[ProgramPrepareFailure, RuntimeCommand]]

/** 题目配置中的头文件源代码，filename 是写入工作目录的 include 文件名。 */
final case class ProgramHeaderSource(
  filename: String,
  sourceCode: SubmissionSourceCode
)

/** 程序准备阶段的失败分类，区分用户编译错误和 judger/工具链系统错误。 */
sealed trait ProgramPrepareFailure

/** 提供程序准备失败的具体 ADT 构造。 */
object ProgramPrepareFailure:
  case object CompileError extends ProgramPrepareFailure
  final case class SystemError(reason: JudgeFailureReason) extends ProgramPrepareFailure

/** runtime 与 runner 共享的文件、进程和结果构造工具。 */
object JudgeRuntimeSupport:
  /** 创建临时工作目录并在使用后递归删除；目录会尽量设置为 sandbox 可访问。 */
  def withWorkingDirectory[A](workRoot: Path, prefix: String)(use: Path => IO[A]): IO[A] =
    IO.blocking {
      Files.createDirectories(workRoot)
      val path = Files.createTempDirectory(workRoot, prefix)
      // 注意：isolate 以独立用户访问 /box 绑定目录，这里放宽临时目录权限是为了让 sandbox 内进程读写。
      ensureSandboxAccessible(path)
      path
    }.bracket(use)(deleteRecursively)

  /** 在绝对路径或 PATH 中查找可执行文件，并解析为真实路径。 */
  def resolveExecutable(command: String): Option[String] =
    val commandPath = Path.of(command)
    if commandPath.isAbsolute && Files.isExecutable(commandPath) then resolveRealExecutablePath(commandPath)
    else
      sys.env
        .get("PATH")
        .toList
        .flatMap(_.split(java.io.File.pathSeparator).toList)
        .map(pathEntry => Path.of(pathEntry).resolve(command))
        .find(path => Files.isRegularFile(path) && Files.isExecutable(path))
        .flatMap(resolveRealExecutablePath)

  /** 判断宿主可执行文件路径是否能在 isolate 默认挂载中直接访问。 */
  def isSandboxVisibleExecutable(path: String): Boolean =
    path.startsWith("/usr/") || path == "/usr" || path.startsWith("/bin/") || path == "/bin"

  /** 在宿主机直接运行编译器等准备阶段进程，并用 prlimit 做粗粒度资源限制。 */
  def runHostProcess(
    command: String,
    args: List[String],
    cwd: Path,
    stdin: Option[Array[Byte]],
    limits: SandboxLimits,
    stdoutName: String,
    stderrName: String
  ): IO[ProcessResult] =
    IO.blocking {
      val stdoutPath = cwd.resolve(stdoutName)
      val stderrPath = cwd.resolve(stderrName)
      Files.deleteIfExists(stdoutPath)
      Files.deleteIfExists(stderrPath)
      val builder = new ProcessBuilder(
        (
          List(
            "prlimit",
            s"--as=${limits.memoryLimitKb.value * 1024L}",
            s"--cpu=${math.max(1L, limits.timeLimit.value / 1000L)}",
            "--"
          ) ++ (command :: args)
        )*
      )
      builder.directory(cwd.toFile)
      builder.redirectOutput(stdoutPath.toFile)
      builder.redirectError(stderrPath.toFile)
      val process = builder.start()

      stdin.foreach { bytes =>
        val stream = process.getOutputStream
        try stream.write(bytes)
        finally stream.close()
      }
      if stdin.isEmpty then process.getOutputStream.close()

      val completed = process.waitFor(limits.wallTimeLimit.value, TimeUnit.MILLISECONDS)
      if !completed then
        process.destroyForcibly()
        process.waitFor(5, TimeUnit.SECONDS)

      ProcessResult(
        exitCode = if completed then Some(process.exitValue()) else None,
        isolateStatus = None,
        isolateMessage = None,
        stdout = readOptionalFile(stdoutPath),
        stderr = readOptionalFile(stderrPath),
        timedOut = !completed,
        timeUsedMs = None,
        wallTimeUsedMs = None,
        memoryUsedKb = None
      )
    }

  /** 确认准备阶段产物存在且可执行；失败会以异常形式进入系统错误路径。 */
  def ensureExecutableExists(path: Path): IO[Unit] =
    IO.blocking {
      if !Files.exists(path) then
        throw RuntimeException(s"Prepared executable was not produced at ${path.toAbsolutePath}.")
      if !Files.isRegularFile(path) then
        throw RuntimeException(s"Prepared executable path is not a regular file: ${path.toAbsolutePath}.")
      if !Files.isExecutable(path) then
        throw RuntimeException(s"Prepared executable is not executable: ${path.toAbsolutePath}.")
    }

  /** 构造一个已完成的整题回报，用于无法逐点执行但可给出统一 verdict 的场景。 */
  def taskCompleted(task: JudgeTask, verdict: SubmissionVerdict): ReportJudgeResultRequest =
    taskResult(task, SubmissionStatus.Completed, verdict, None)

  /** 构造一个系统失败的整题回报，并为每个子任务填充空测试点结果。 */
  def taskSystemError(task: JudgeTask, reason: JudgeFailureReason): ReportJudgeResultRequest =
    taskResult(task, SubmissionStatus.Failed, SubmissionVerdict.SystemError, Some(reason))

  private def taskResult(
    task: JudgeTask,
    status: SubmissionStatus,
    verdict: SubmissionVerdict,
    reason: Option[JudgeFailureReason]
  ): ReportJudgeResultRequest =
    val summary = resultSummary(verdict, reason)
    val subtasks = task.subtasks.map { subtask =>
      JudgeSubtaskResult(
        index = subtask.index,
        label = subtask.label,
        baseResult = summary,
        worstResult = summary,
        testcases = Nil
      )
    }
    ReportJudgeResultRequest(
      status = status,
      judgeResult = Some(
        JudgeResult(
          baseResult = summary,
          worstResult = summary,
          subtasks = subtasks
        )
      )
    )

  private def resultSummary(verdict: SubmissionVerdict, reason: Option[JudgeFailureReason]): JudgeResultSummary =
    verdict match
      case SubmissionVerdict.SystemError =>
        JudgeResultSummary.failed(reason.getOrElse(JudgeFailureReason.SystemError))
      case other =>
        JudgeResultSummary.nonSystem(BigDecimal(0), other, None, None)

  /** 组合用户可见的失败详情，优先包含 stderr/stdout，必要时附带 isolate 元信息。 */
  def renderDetail(detail: String, result: ProcessResult, includeIsolateDetail: Boolean = false): String =
    val isolateDetail =
      if includeIsolateDetail then
        List(
          result.isolateStatus.map(status => s"isolate status: $status"),
          result.isolateMessage.map(message => s"isolate message: $message")
        ).flatten
      else Nil

    List(
      Some(detail),
      Option.when(isolateDetail.nonEmpty)(isolateDetail.mkString("\n")),
      namedSection("stderr", result.stderr),
      namedSection("stdout", result.stdout)
    ).flatten.mkString("\n\n")

  /** 从两个候选输出中选第一个非空文本，否则返回兜底说明。 */
  def nonEmptyOrFallback(primary: String, secondary: String, fallback: String): String =
    val first = primary.trim
    if first.nonEmpty then first
    else
      val second = secondary.trim
      if second.nonEmpty then second else fallback

  private def deleteRecursively(path: Path): IO[Unit] =
    IO.blocking {
      if Files.exists(path) then
        val paths = Files.walk(path)
        try paths.sorted(java.util.Comparator.reverseOrder()).forEach(currentPath => Files.deleteIfExists(currentPath))
        finally paths.close()
    }.void.handleError(_ => ())

  private def ensureSandboxAccessible(path: Path): Unit =
    scala.util.Try {
      Files.setPosixFilePermissions(path, PosixFilePermissions.fromString("rwxrwxrwx"))
    }
    ()

  private def resolveRealExecutablePath(path: Path): Option[String] =
    scala.util.Try(path.toRealPath().toString).toOption

  private def readOptionalFile(path: Path): String =
    if Files.exists(path) then Files.readString(path, StandardCharsets.UTF_8) else ""

  private def namedSection(label: String, value: String): Option[String] =
    Option.when(value.trim.nonEmpty)(s"$label:\n${value.trim}")
