package judger.infra

import cats.effect.IO
import judgeprotocol.objects.{SubmissionLanguage, SubmissionSourceCode}
import judgeprotocol.objects.response.JudgeFailureReason
import judger.config.AppConfig
import judger.infra.JudgeRuntimeSupport.*
import judger.objects.{RuntimeCommand, SandboxLimits}

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path}

object Cpp17Runtime extends JudgeRuntime:
  private val CompileLimits = SandboxLimits.runtime(timeLimitMs = 15000L, memoryLimitMb = 2048)

  override val language: SubmissionLanguage = SubmissionLanguage.Cpp17

  override def prepare(
    role: String,
    sourceCode: SubmissionSourceCode,
    config: AppConfig,
    workingDirectory: Path
  ): IO[Either[ProgramPrepareFailure, RuntimeCommand]] =
    resolveCompilerPath(config).flatMap {
      case Left(_) =>
        IO.pure(Left(ProgramPrepareFailure.SystemError(JudgeFailureReason.JudgerRuntimeFailed)))
      case Right(compilerPath) =>
        val safeRole = IsolateSandbox.sanitizeFilename(role)
        val sourceName = s"main-$safeRole.cpp"
        val executableName = s"main-$safeRole"
        val sourceFile = workingDirectory.resolve(sourceName)
        for
          _ <- IO.blocking(Files.writeString(sourceFile, sourceCode.value, StandardCharsets.UTF_8))
          compileResult <- runHostProcess(
            command = compilerPath,
            args = List(sourceName, "-o", executableName, "-O2", "-std=c++17"),
            cwd = workingDirectory,
            stdin = None,
            limits = CompileLimits,
            stdoutName = s".$executableName.compile.stdout",
            stderrName = s".$executableName.compile.stderr"
          )
          result <-
            if compileResult.timedOut then
              IO.pure(Left(ProgramPrepareFailure.CompileError))
            else if compileResult.exitCode.contains(127) then
              IO.pure(Left(ProgramPrepareFailure.SystemError(JudgeFailureReason.JudgerRuntimeFailed)))
            else if compileResult.exitCode.getOrElse(-1) != 0 then
              IO.pure(Left(ProgramPrepareFailure.CompileError))
            else
              ensureExecutableExists(workingDirectory.resolve(executableName)).as(Right(RuntimeCommand(s"/box/$executableName", Nil, processLimit = 1)))
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
