package judger.infra

import cats.effect.IO
import cats.syntax.all.*
import judgeprotocol.objects.{SubmissionStatus, SubmissionVerdict}
import judgeprotocol.objects.request.ReportJudgeResultRequest
import judgeprotocol.objects.response.{JudgeFailureReason, JudgeResult, JudgeSubtaskResult, JudgeTask, JudgeTaskFileRef, JudgeTaskSubtask, JudgeTaskTestcase, JudgeTestcaseResult}
import judger.config.AppConfig
import judger.infra.JudgeRuntimeSupport.*
import judger.objects.{ProcessResult, RuntimeCommand, SandboxExecutionRequest, SandboxLimits}

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path}
import scala.util.matching.Regex

object JudgeExecutor:
  def judge(
    task: JudgeTask,
    config: AppConfig,
    problemDataCache: ProblemDataCache,
    runtime: JudgeRuntime
  ): IO[ReportJudgeResultRequest] =
    withWorkingDirectory(config.workRoot, "qiwen-judger-") { workingDirectory =>
      IsolateSandbox.resource(config) { sandbox =>
        runtime.prepare(task, config, workingDirectory).flatMap {
          case Left(result) =>
            IO.pure(result)
          case Right(command) =>
            prepareCheckers(task, config, workingDirectory, problemDataCache).flatMap {
              case Left(result) => IO.pure(result)
              case Right(checkers) => judgeSubtasks(task, workingDirectory, sandbox, problemDataCache, command, checkers)
            }
        }
      }
    }.handleError { _ =>
      taskSystemError(task, JudgeFailureReason.JudgerRuntimeFailed)
    }

  private def judgeSubtasks(
    task: JudgeTask,
    workingDirectory: Path,
    sandbox: IsolateSandbox,
    problemDataCache: ProblemDataCache,
    command: RuntimeCommand,
    compiledCheckers: Map[String, RuntimeCommand]
  ): IO[ReportJudgeResultRequest] =
    task.subtasks.traverse(subtask => judgeSubtask(task, subtask, workingDirectory, sandbox, problemDataCache, command, compiledCheckers)).map { subtasks =>
      val result = aggregateTask(task, subtasks)
      ReportJudgeResultRequest(
        status = if containsSystemError(result) then SubmissionStatus.Failed else SubmissionStatus.Completed,
        judgeResult = Some(result)
      )
    }

  private def judgeSubtask(
    task: JudgeTask,
    subtask: JudgeTaskSubtask,
    workingDirectory: Path,
    sandbox: IsolateSandbox,
    problemDataCache: ProblemDataCache,
    command: RuntimeCommand,
    compiledCheckers: Map[String, RuntimeCommand]
  ): IO[JudgeSubtaskResult] =
    subtask.testcases.traverse(testcase => judgeTestcase(task, subtask.name, testcase, workingDirectory, sandbox, problemDataCache, command, compiledCheckers)).map { testcases =>
      aggregateSubtask(subtask, testcases)
    }

  private def judgeTestcase(
    task: JudgeTask,
    subtaskName: String,
    testcase: JudgeTaskTestcase,
    workingDirectory: Path,
    sandbox: IsolateSandbox,
    problemDataCache: ProblemDataCache,
    command: RuntimeCommand,
    compiledCheckers: Map[String, RuntimeCommand]
  ): IO[JudgeTestcaseResult] =
    loadTestcaseData(task, testcase, problemDataCache).flatMap {
      case Left(reason) =>
        IO.pure(testcaseSystemError(testcase, reason))
      case Right((input, answerBytes)) =>
        for
          runResult <- sandbox.run(
            SandboxExecutionRequest(
              phase = s"run-${subtaskName}-${testcase.name.value}",
              command = command.command,
              args = command.args,
              stdin = Some(input),
              limits = SandboxLimits.runtime(testcase.limits.timeMs.value.toLong, testcase.limits.memoryMb.value),
              processLimit = command.processLimit
            ),
            workingDirectory
          )
          result <-
            if runResult.timedOut then
              IO.pure(testcaseResult(testcase, BigDecimal(0), SubmissionVerdict.TimeLimitExceeded, None, None, runResult))
            else if runResult.exitCode.getOrElse(-1) != 0 then
              IO.pure(testcaseResult(testcase, BigDecimal(0), SubmissionVerdict.RuntimeError, None, None, runResult))
            else
              scoreWithChecker(task, testcase, workingDirectory, sandbox, input, runResult.stdout, answerBytes, compiledCheckers).map {
                case Right(checkerScore) =>
                  testcaseResult(
                    testcase,
                    checkerScore.score,
                    if checkerScore.score == BigDecimal(1) then SubmissionVerdict.Accepted else SubmissionVerdict.WrongAnswer,
                    checkerScore.message,
                    None,
                    runResult
                  )
                case Left(reason) =>
                  testcaseResult(testcase, BigDecimal(0), SubmissionVerdict.SystemError, None, Some(reason), runResult)
              }
        yield result
    }

  private def testcaseResult(
    testcase: JudgeTaskTestcase,
    score: BigDecimal,
    verdict: SubmissionVerdict,
    message: Option[String],
    reason: Option[JudgeFailureReason],
    runResult: ProcessResult
  ): JudgeTestcaseResult =
    JudgeTestcaseResult(testcase.name.value, clampScore(score), verdict, message, reason, runResult.timeUsedMs, runResult.memoryUsedKb)

  private def testcaseSystemError(testcase: JudgeTaskTestcase, reason: JudgeFailureReason): JudgeTestcaseResult =
    JudgeTestcaseResult(testcase.name.value, BigDecimal(0), SubmissionVerdict.SystemError, None, Some(reason), None, None)

  private def loadTestcaseData(
    task: JudgeTask,
    testcase: JudgeTaskTestcase,
    problemDataCache: ProblemDataCache
  ): IO[Either[JudgeFailureReason, (Array[Byte], Array[Byte])]] =
    (for
      input <- testcase.input.traverse(ref => problemDataCache.loadBytes(task.problemSlug, task.problemDataVersion, ref)).map(_.getOrElse(Array.emptyByteArray))
      answerBytes <- problemDataCache.loadBytes(task.problemSlug, task.problemDataVersion, testcase.answer)
    yield (input, answerBytes)).attempt.map {
      case Right(data) => Right(data)
      case Left(_) => Left(JudgeFailureReason.ProblemDataLoadFailed)
    }

  private def scoreWithChecker(
    task: JudgeTask,
    testcase: JudgeTaskTestcase,
    workingDirectory: Path,
    sandbox: IsolateSandbox,
    input: Array[Byte],
    contestantOutput: String,
    answerBytes: Array[Byte],
    compiledCheckers: Map[String, RuntimeCommand]
  ): IO[Either[JudgeFailureReason, CheckerScore]] =
    testcase.checker.`type` match
      case "builtin" if testcase.checker.name.contains("exact") =>
        val expectedOutput = String(answerBytes, StandardCharsets.UTF_8)
        IO.pure(Right(CheckerScore(if normalizeOutput(contestantOutput) == normalizeOutput(expectedOutput) then BigDecimal(1) else BigDecimal(0), None)))
      case "cpp" =>
        testcase.checker.source match
          case Some(source) =>
            compiledCheckers.get(source.path) match
              case None => IO.pure(Left(JudgeFailureReason.CheckerCompileFailed))
              case Some(checkerCommand) =>
                val safeName = IsolateSandbox.sanitizeFilename(testcase.name.value)
                val inputPath = workingDirectory.resolve(s"checker-$safeName.in")
                val outputPath = workingDirectory.resolve(s"checker-$safeName.out")
                val answerPath = workingDirectory.resolve(s"checker-$safeName.ans")
                for
                  _ <- IO.blocking {
                    Files.write(inputPath, input)
                    Files.writeString(outputPath, contestantOutput, StandardCharsets.UTF_8)
                    Files.write(answerPath, answerBytes)
                  }
                  checkerResult <- sandbox.run(
                    SandboxExecutionRequest(
                      phase = s"checker-${testcase.name.value}",
                      command = checkerCommand.command,
                      args = checkerCommand.args ++ List(inputPath.getFileName.toString, outputPath.getFileName.toString, answerPath.getFileName.toString),
                      stdin = None,
                      limits = SandboxLimits.runtime(testcase.limits.timeMs.value.toLong, testcase.limits.memoryMb.value),
                      processLimit = checkerCommand.processLimit
                    ),
                    workingDirectory
                  )
                yield parseCheckerResult(checkerResult)
          case None => IO.pure(Left(JudgeFailureReason.SystemError))
      case _ => IO.pure(Left(JudgeFailureReason.SystemError))

  private def prepareCheckers(
    task: JudgeTask,
    config: AppConfig,
    workingDirectory: Path,
    problemDataCache: ProblemDataCache
  ): IO[Either[ReportJudgeResultRequest, Map[String, RuntimeCommand]]] =
    val checkerSources = task.subtasks.flatMap(_.testcases).flatMap(_.checker.source).groupBy(_.path).values.map(_.head).toList
    checkerSources.traverse(source => compileChecker(task, config, workingDirectory, problemDataCache, source).map(_.map(source.path -> _))).map { compiled =>
      compiled.collectFirst { case Left(result) => Left(result) }.getOrElse(Right(compiled.collect { case Right(entry) => entry }.toMap))
    }

  private def compileChecker(
    task: JudgeTask,
    config: AppConfig,
    workingDirectory: Path,
    problemDataCache: ProblemDataCache,
    sourceRef: JudgeTaskFileRef
  ): IO[Either[ReportJudgeResultRequest, RuntimeCommand]] =
    resolveCompilerPath(config).flatMap {
      case Left(_) => IO.pure(Left(taskSystemError(task, JudgeFailureReason.JudgerRuntimeFailed)))
      case Right(compilerPath) =>
        val sourceName = s"checker-${math.abs(sourceRef.path.hashCode)}.cpp"
        val executableName = s"checker-${math.abs(sourceRef.path.hashCode)}"
        problemDataCache.loadBytes(task.problemSlug, task.problemDataVersion, sourceRef).attempt.flatMap {
          case Left(_) =>
            IO.pure(Left(taskSystemError(task, JudgeFailureReason.ProblemDataLoadFailed)))
          case Right(sourceBytes) =>
            for
              _ <- IO.blocking {
                Files.write(workingDirectory.resolve(sourceName), sourceBytes)
                Files.writeString(workingDirectory.resolve("testlib.h"), MinimalTestlibHeader, StandardCharsets.UTF_8)
              }
              compileResult <- runHostProcess(
                command = compilerPath,
                args = List(sourceName, "-o", executableName, "-O2", "-std=c++17", "-I", "."),
                cwd = workingDirectory,
                stdin = None,
                limits = SandboxLimits.runtime(timeLimitMs = 15000L, memoryLimitMb = 2048),
                stdoutName = s".$executableName.compile.stdout",
                stderrName = s".$executableName.compile.stderr"
              )
              result <-
                if compileResult.timedOut then IO.pure(Left(taskSystemError(task, JudgeFailureReason.CheckerCompileFailed)))
                else if compileResult.exitCode.getOrElse(-1) != 0 then IO.pure(Left(taskSystemError(task, JudgeFailureReason.CheckerCompileFailed)))
                else
                  ensureExecutableExists(workingDirectory.resolve(executableName)).attempt.map {
                    case Right(_) => Right(RuntimeCommand(s"/box/$executableName", Nil, processLimit = 1))
                    case Left(_) => Left(taskSystemError(task, JudgeFailureReason.CheckerCompileFailed))
                  }
            yield result
        }
    }

  private def resolveCompilerPath(config: AppConfig): IO[Either[String, String]] =
    IO.blocking {
      resolveExecutable(config.cxx) match
        case None => Left(s"Compiler '${config.cxx}' was not found on the judger host.")
        case Some(path) if isSandboxVisibleExecutable(path) => Right(path)
        case Some(path) => Left(s"Compiler '$path' is not visible inside isolate.")
    }

  private val ScoreNumber: Regex = """(?<![\w.])(?:0(?:\.\d+)?|1(?:\.0+)?)(?![\w.])""".r

  private final case class CheckerScore(score: BigDecimal, message: Option[String])

  private def parseCheckerResult(result: ProcessResult): Either[JudgeFailureReason, CheckerScore] =
    if result.timedOut then Left(JudgeFailureReason.CheckerRuntimeFailed)
    else if result.exitCode.contains(0) then Right(CheckerScore(BigDecimal(1), checkerReport(result)))
    else
      ScoreNumber.findFirstIn(s"${result.stdout}\n${result.stderr}") match
        case Some(raw) => Right(CheckerScore(clampScore(BigDecimal(raw)), checkerReport(result)))
        case None => Left(JudgeFailureReason.CheckerRuntimeFailed)

  private def checkerReport(result: ProcessResult): Option[String] =
    List(result.stderr.trim, result.stdout.trim).find(_.nonEmpty)

  private def aggregateSubtask(subtask: JudgeTaskSubtask, testcases: List[JudgeTestcaseResult]): JudgeSubtaskResult =
    val score = aggregateScore(subtask.aggregation.score, testcases.map(_.score), subtask.testcases.map(_.scoreRatio))
    val verdict = aggregateVerdict(score, testcases.map(result => result.score -> result.verdict))
    val reason = Option.when(verdict == SubmissionVerdict.SystemError)(
      testcases.find(_.verdict == SubmissionVerdict.SystemError).flatMap(_.reason).getOrElse(JudgeFailureReason.SystemError)
    )
    JudgeSubtaskResult(subtask.name, score, verdict, aggregateUsage(subtask.aggregation.time, testcases.flatMap(_.timeUsedMs)), aggregateUsage(subtask.aggregation.memory, testcases.flatMap(_.memoryUsedKb)), reason, testcases)

  private def aggregateTask(task: JudgeTask, subtasks: List[JudgeSubtaskResult]): JudgeResult =
    val rawScore = aggregateScore(task.aggregation.score, subtasks.map(_.score), task.subtasks.map(_.scoreRatio))
    val score = roundFinalScore(rawScore, task.roundingScale)
    val verdict = aggregateVerdict(score, subtasks.map(result => result.score -> result.verdict))
    val reason = Option.when(verdict == SubmissionVerdict.SystemError)(
      subtasks.find(_.verdict == SubmissionVerdict.SystemError).flatMap(_.reason).getOrElse(JudgeFailureReason.SystemError)
    )
    JudgeResult(score, verdict, reason, aggregateUsage(task.aggregation.time, subtasks.flatMap(_.timeUsedMs)), aggregateUsage(task.aggregation.memory, subtasks.flatMap(_.memoryUsedKb)), subtasks)

  private def aggregateScore(kind: String, scores: List[BigDecimal], ratios: List[BigDecimal]): BigDecimal =
    kind match
      case "min" => if scores.isEmpty then BigDecimal(0) else scores.min
      case "sum" => scores.zip(ratios).map { case (score, ratio) => score * ratio }.sum
      case _ => BigDecimal(0)

  private def aggregateUsage(kind: String, values: List[Long]): Option[Long] =
    if values.isEmpty then None
    else
      kind match
        case "sum" => Some(values.sum)
        case _ => Some(values.max)

  private def aggregateVerdict(score: BigDecimal, children: List[(BigDecimal, SubmissionVerdict)]): SubmissionVerdict =
    if score == BigDecimal(1) then SubmissionVerdict.Accepted
    else children.minByOption(_._1).map(_._2).getOrElse(SubmissionVerdict.SystemError)

  private def roundFinalScore(score: BigDecimal, scale: Int): BigDecimal =
    val roundedUp = score.setScale(scale, BigDecimal.RoundingMode.CEILING)
    if score < BigDecimal(1) && roundedUp >= BigDecimal(1) then BigDecimal(1) - BigDecimal(java.math.BigDecimal.ONE.movePointLeft(scale))
    else roundedUp

  private def clampScore(score: BigDecimal): BigDecimal =
    score.max(BigDecimal(0)).min(BigDecimal(1))

  private def containsSystemError(result: JudgeResult): Boolean =
    result.verdict == SubmissionVerdict.SystemError ||
      result.subtasks.exists(subtask => subtask.verdict == SubmissionVerdict.SystemError || subtask.testcases.exists(_.verdict == SubmissionVerdict.SystemError))

  private def normalizeOutput(value: String): String =
    value.replace("\r\n", "\n").replace('\r', '\n').stripTrailing()

  private val MinimalTestlibHeader: String =
    """#pragma once
      |#include <bits/stdc++.h>
      |using namespace std;
      |
      |enum TResult { _ok, _wa, _pe, _fail };
      |
      |class InStream {
      |  ifstream file;
      |public:
      |  InStream() = default;
      |  explicit InStream(const char* path) { file.open(path); }
      |  void init(const char* path) { file.open(path); }
      |  int readInt() { int value; file >> value; return value; }
      |  long long readLong() { long long value; file >> value; return value; }
      |  double readDouble() { double value; file >> value; return value; }
      |  string readString() { string value; file >> value; return value; }
      |  string readLine() { string value; getline(file, value); return value; }
      |  bool eof() { return file.eof(); }
      |  bool seekEof() { file >> ws; return file.eof(); }
      |};
      |
      |static InStream inf;
      |static InStream ouf;
      |static InStream ans;
      |
      |inline void registerTestlibCmd(int argc, char* argv[]) {
      |  if (argc < 4) {
      |    cerr << "checker requires input, output, and answer paths";
      |    exit(3);
      |  }
      |  inf.init(argv[1]);
      |  ouf.init(argv[2]);
      |  ans.init(argv[3]);
      |}
      |
      |inline void quitf(TResult result, const char* format, ...) {
      |  if (result == _ok) exit(0);
      |  if (result == _wa || result == _pe) {
      |    cout << "0";
      |    exit(1);
      |  }
      |  exit(3);
      |}
      |
      |inline void quitp(double score, const char* format = "", ...) {
      |  if (score >= 1.0) exit(0);
      |  cout << max(0.0, min(1.0, score));
      |  exit(1);
      |}
      |""".stripMargin
