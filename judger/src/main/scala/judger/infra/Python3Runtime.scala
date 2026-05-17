package judger.infra

import cats.effect.IO
import judgeprotocol.model.{JudgeTask, ReportJudgeResultRequest, SubmissionLanguage, SubmissionVerdict}
import judger.config.AppConfig
import judger.infra.JudgeRuntimeSupport.*

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path}

object Python3Runtime extends JudgeRuntime:
  private val CompileLimits = SandboxLimits.runtime(timeLimitMs = 15000L, memoryLimitMb = 2048)

  override val language: SubmissionLanguage = SubmissionLanguage.Python3

  override def prepare(
    task: JudgeTask,
    config: AppConfig,
    workingDirectory: Path
  ): IO[Either[ReportJudgeResultRequest, RuntimeCommand]] =
    resolveInterpreterPath(config).flatMap {
      case Left(message) =>
        IO.pure(Left(systemError(message)))
      case Right(interpreterPath) =>
        val sourceFile = workingDirectory.resolve("main.py")
        val bytecodeFile = workingDirectory.resolve("main.pyc")
        for
          _ <- IO.blocking(Files.writeString(sourceFile, task.sourceCode.value, StandardCharsets.UTF_8))
          compileResult <- runHostProcess(
            command = interpreterPath,
            args = List("-c", "import py_compile; py_compile.compile('main.py', cfile='main.pyc', doraise=True)"),
            cwd = workingDirectory,
            stdin = None,
            limits = CompileLimits,
            stdoutName = ".pycompile.stdout",
            stderrName = ".pycompile.stderr"
          )
          result <-
            if compileResult.timedOut then
              IO.pure(
                Left(
                  completed(
                    SubmissionVerdict.CompileError,
                    s"Python bytecode compilation exceeded the judger resource limits (${CompileLimits.memoryLimitKb.value / 1024L} MB, ${CompileLimits.timeLimit.value} ms)."
                  )
                )
              )
            else if compileResult.exitCode.getOrElse(-1) != 0 then
              IO.pure(Left(completed(SubmissionVerdict.CompileError, formatCompileError(interpreterPath, compileResult))))
            else
              ensureBytecodeExists(bytecodeFile).as(Right(RuntimeCommand(interpreterPath, List("/box/main.pyc"), processLimit = 1)))
        yield result
    }

  private def resolveInterpreterPath(config: AppConfig): IO[Either[String, String]] =
    IO.blocking {
      resolveExecutable(config.python3) match
        case None =>
          Left(s"Python 3 interpreter '${config.python3}' was not found on the judger host.")
        case Some(path) if isSandboxVisibleExecutable(path) =>
          Right(path)
        case Some(path) =>
          Left(
            s"Python 3 interpreter '$path' is not visible inside isolate. " +
              "Use an interpreter under /usr or /bin, or adjust the judger configuration."
          )
    }

  private def ensureBytecodeExists(path: Path): IO[Unit] =
    IO.blocking {
      if !Files.exists(path) then
        throw RuntimeException(s"Python bytecode was not produced at ${path.toAbsolutePath}.")
      if !Files.isRegularFile(path) then
        throw RuntimeException(s"Python bytecode path is not a regular file: ${path.toAbsolutePath}.")
    }

  private def formatCompileError(interpreterPath: String, result: ProcessResult): String =
    val exitCode = result.exitCode.getOrElse(-1)
    if exitCode == 127 then
      renderDetail(s"Python 3 interpreter '$interpreterPath' was not found on the judger host (exit status 127).", result)
    else
      nonEmptyOrFallback(
        result.stderr,
        result.stdout,
        s"Python bytecode compilation failed with exit status $exitCode using $interpreterPath."
      )
