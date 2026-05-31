package judger.infra

import cats.effect.IO
import judgeprotocol.objects.{SubmissionLanguage, SubmissionVerdict}
import judgeprotocol.objects.request.ReportJudgeResultRequest
import judgeprotocol.objects.response.{JudgeFailureReason, JudgeTask}
import judger.config.AppConfig
import judger.infra.JudgeRuntimeSupport.*
import judger.objects.{RuntimeCommand, SandboxLimits}

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
      case Left(_) =>
        IO.pure(Left(taskSystemError(task, JudgeFailureReason.JudgerRuntimeFailed)))
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
              IO.pure(Left(taskCompleted(task, SubmissionVerdict.CompileError)))
            else if compileResult.exitCode.contains(127) then
              IO.pure(Left(taskSystemError(task, JudgeFailureReason.JudgerRuntimeFailed)))
            else if compileResult.exitCode.getOrElse(-1) != 0 then
              IO.pure(Left(taskCompleted(task, SubmissionVerdict.CompileError)))
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
