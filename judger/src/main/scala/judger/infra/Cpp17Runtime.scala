package judger.infra

import cats.effect.IO
import judgeprotocol.objects.{SubmissionLanguage, SubmissionVerdict}
import judgeprotocol.objects.request.ReportJudgeResultRequest
import judgeprotocol.objects.response.JudgeTask
import judger.config.AppConfig
import judger.infra.JudgeRuntimeSupport.*
import judger.objects.{ProcessResult, RuntimeCommand, SandboxLimits}

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path}

object Cpp17Runtime extends JudgeRuntime:
  private val CompileLimits = SandboxLimits.runtime(timeLimitMs = 15000L, memoryLimitMb = 2048)

  override val language: SubmissionLanguage = SubmissionLanguage.Cpp17

  override def prepare(
    task: JudgeTask,
    config: AppConfig,
    workingDirectory: Path
  ): IO[Either[ReportJudgeResultRequest, RuntimeCommand]] =
    resolveCompilerPath(config).flatMap {
      case Left(message) =>
        IO.pure(Left(systemError(message)))
      case Right(compilerPath) =>
        val sourceFile = workingDirectory.resolve("main.cpp")
        for
          _ <- IO.blocking(Files.writeString(sourceFile, task.sourceCode.value, StandardCharsets.UTF_8))
          compileResult <- runHostProcess(
            command = compilerPath,
            args = List("main.cpp", "-o", "main", "-O2", "-std=c++17"),
            cwd = workingDirectory,
            stdin = None,
            limits = CompileLimits,
            stdoutName = ".compile.stdout",
            stderrName = ".compile.stderr"
          )
          result <-
            if compileResult.timedOut then
              IO.pure(
                Left(
                  completed(
                    SubmissionVerdict.CompileError,
                    s"Compilation exceeded the judger resource limits (${CompileLimits.memoryLimitKb.value / 1024L} MB, ${CompileLimits.timeLimit.value} ms)."
                  )
                )
              )
            else if compileResult.exitCode.getOrElse(-1) != 0 then
              IO.pure(Left(completed(SubmissionVerdict.CompileError, formatCompileError(compilerPath, compileResult))))
            else
              ensureExecutableExists(workingDirectory.resolve("main")).as(Right(RuntimeCommand("/box/main", Nil, processLimit = 1)))
        yield result
    }

  private def resolveCompilerPath(config: AppConfig): IO[Either[String, String]] =
    IO.blocking {
      resolveExecutable(config.cxx) match
        case None =>
          Left(s"Compiler '${config.cxx}' was not found on the judger host.")
        case Some(path) if isSandboxVisibleExecutable(path) =>
          Right(path)
        case Some(path) =>
          Left(
            s"Compiler '$path' is not visible inside isolate. " +
              "Use a compiler under /usr or /bin, or adjust the judger configuration."
          )
    }

  private def formatCompileError(compilerPath: String, result: ProcessResult): String =
    val exitCode = result.exitCode.getOrElse(-1)
    if exitCode == 127 then
      renderDetail(s"Compiler '$compilerPath' was not found on the judger host (exit status 127).", result)
    else
      nonEmptyOrFallback(
        result.stderr,
        result.stdout,
        s"Compilation failed with exit status $exitCode using $compilerPath."
      )
