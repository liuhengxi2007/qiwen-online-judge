package judger.infra

import cats.effect.IO
import cats.syntax.all.*
import judgeprotocol.objects.response.{JudgeFailureReason, JudgeTask, JudgeTaskFilePath, JudgeTaskTestcase}
import judger.objects.{RuntimeCommand, SandboxLimits, SandboxRunSpec, SandboxStdin, SandboxStdout}

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path}
import scala.util.Try

object CheckerRunner:
  private[infra] final case class CheckerScore(score: BigDecimal, message: Option[String])

  private[infra] def score(
    task: JudgeTask,
    testcase: JudgeTaskTestcase,
    workingDirectory: Path,
    sandbox: SandboxSession,
    input: Array[Byte],
    contestantOutput: String,
    answerBytes: Option[Array[Byte]],
    compiledCheckers: Map[JudgeTaskFilePath, RuntimeCommand]
  ): IO[Either[JudgeFailureReason, CheckerScore]] =
    testcase.checker.`type` match
      case "builtin" if testcase.checker.name.contains("exact") =>
        answerBytes match
          case None => IO.pure(Left(JudgeFailureReason.CheckerRuntimeFailed))
          case Some(bytes) =>
            val expectedOutput = String(bytes, StandardCharsets.UTF_8)
            IO.pure(Right(CheckerScore(if normalizeOutput(contestantOutput) == normalizeOutput(expectedOutput) then BigDecimal(1) else BigDecimal(0), None)))
      case "builtin" if testcase.checker.name.contains("echo") =>
        IO.pure(parseCheckerStdout(contestantOutput))
      case "cpp17" | "cpp" =>
        testcase.checker.source match
          case Some(source) =>
            compiledCheckers.get(source.path) match
              case None => IO.pure(Left(JudgeFailureReason.CheckerCompileFailed))
              case Some(checkerCommand) =>
                val safeName = s"${testcase.index}-${math.abs(source.path.value.hashCode)}"
                val inputPath = workingDirectory.resolve(s"checker-$safeName.in")
                val outputPath = workingDirectory.resolve(s"checker-$safeName.out")
                val answerPath = workingDirectory.resolve(s"checker-$safeName.ans")
                for
                  _ <- IO.blocking {
                    Files.write(inputPath, input)
                    Files.writeString(outputPath, contestantOutput, StandardCharsets.UTF_8)
                    answerBytes match
                      case Some(bytes) => Files.write(answerPath, bytes)
                      case None => Files.deleteIfExists(answerPath)
                  }
                  checkerResult <- sandbox.run(
                    SandboxRunSpec(
                      phase = s"checker-${testcase.index}",
                      command = RuntimeCommand(
                        checkerCommand.command,
                        checkerCommand.args ++ List(
                          inputPath.getFileName.toString,
                          outputPath.getFileName.toString,
                          answerBytes.fold("/dev/null")(_ => answerPath.getFileName.toString)
                        ),
                        checkerCommand.processLimit
                      ),
                      stdin = SandboxStdin.Empty,
                      stdout = SandboxStdout.Capture,
                      limits = SandboxLimits.runtime(testcase.limits.timeMs.value.toLong, testcase.limits.memoryMb.value)
                    ),
                    workingDirectory
                  )
                yield parseCheckerResult(checkerResult)
          case None => IO.pure(Left(JudgeFailureReason.SystemError))
      case _ => IO.pure(Left(JudgeFailureReason.SystemError))

  private[infra] def parseCheckerResult(result: judger.objects.ProcessResult): Either[JudgeFailureReason, CheckerScore] =
    if result.timedOut || result.exitCode.getOrElse(-1) != 0 then Left(JudgeFailureReason.CheckerRuntimeFailed)
    else parseCheckerStdout(result.stdout)

  private[infra] def parseCheckerStdout(stdout: String): Either[JudgeFailureReason, CheckerScore] =
    val trimmed = stdout.trim
    val firstWhitespace = trimmed.indexWhere(_.isWhitespace)
    val (rawScore, rawMessage) =
      if firstWhitespace < 0 then trimmed -> ""
      else trimmed.take(firstWhitespace) -> trimmed.drop(firstWhitespace).trim
    Try(BigDecimal(rawScore)).toEither
      .leftMap(_ => JudgeFailureReason.CheckerRuntimeFailed)
      .flatMap { score =>
        if score >= BigDecimal(0) && score <= BigDecimal(1) then Right(CheckerScore(score, Option.when(rawMessage.nonEmpty)(rawMessage)))
        else Left(JudgeFailureReason.CheckerRuntimeFailed)
      }

  private[infra] def normalizeOutput(value: String): String =
    value.replace("\r\n", "\n").replace('\r', '\n').stripTrailing()
