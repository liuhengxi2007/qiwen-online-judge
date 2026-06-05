package judger.infra

import cats.effect.IO
import judgeprotocol.objects.{SubmissionLanguage, SubmissionSourceCode}
import judgeprotocol.objects.response.JudgeFailureReason
import judger.config.AppConfig
import judger.infra.JudgeRuntimeSupport.*
import judger.objects.{RuntimeCommand, SandboxLimits}

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path}

object Python3Runtime extends JudgeRuntime:
  private val CompileLimits = SandboxLimits.runtime(timeLimitMs = 15000L, memoryLimitMb = 2048)

  override val language: SubmissionLanguage = SubmissionLanguage.Python3

  override def prepare(
    role: String,
    sourceCode: SubmissionSourceCode,
    config: AppConfig,
    workingDirectory: Path
  ): IO[Either[ProgramPrepareFailure, RuntimeCommand]] =
    resolveInterpreterPath(config).flatMap {
      case Left(_) =>
        IO.pure(Left(ProgramPrepareFailure.SystemError(JudgeFailureReason.JudgerRuntimeFailed)))
      case Right(interpreterPath) =>
        val safeRole = IsolateSandbox.sanitizeFilename(role)
        val sourceName = s"main-$safeRole.py"
        val bytecodeName = s"main-$safeRole.pyc"
        val sourceFile = workingDirectory.resolve(sourceName)
        val bytecodeFile = workingDirectory.resolve(bytecodeName)
        for
          _ <- IO.blocking(Files.writeString(sourceFile, sourceCode.value, StandardCharsets.UTF_8))
          compileResult <- runHostProcess(
            command = interpreterPath,
            args = List("-c", s"import py_compile; py_compile.compile('$sourceName', cfile='$bytecodeName', doraise=True)"),
            cwd = workingDirectory,
            stdin = None,
            limits = CompileLimits,
            stdoutName = s".$bytecodeName.compile.stdout",
            stderrName = s".$bytecodeName.compile.stderr"
          )
          result <-
            if compileResult.timedOut then
              IO.pure(Left(ProgramPrepareFailure.CompileError))
            else if compileResult.exitCode.contains(127) then
              IO.pure(Left(ProgramPrepareFailure.SystemError(JudgeFailureReason.JudgerRuntimeFailed)))
            else if compileResult.exitCode.getOrElse(-1) != 0 then
              IO.pure(Left(ProgramPrepareFailure.CompileError))
            else
              ensureBytecodeExists(bytecodeFile).as(Right(RuntimeCommand(interpreterPath, List(s"/box/$bytecodeName"), processLimit = 1)))
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
