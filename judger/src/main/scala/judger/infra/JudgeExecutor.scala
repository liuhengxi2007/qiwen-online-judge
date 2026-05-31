package judger.infra

import cats.effect.IO
import cats.syntax.all.*
import judgeprotocol.objects.{SubmissionStatus, SubmissionVerdict}
import judgeprotocol.objects.request.ReportJudgeResultRequest
import judgeprotocol.objects.response.{JudgeResult, JudgeSubtaskResult, JudgeTask, JudgeTaskFileRef, JudgeTaskSubtask, JudgeTaskTestcase, JudgeTestcaseResult}
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
    }.handleError { error =>
      val message = Option(error.getMessage).map(_.trim).filter(_.nonEmpty).getOrElse(error.getClass.getName)
      taskSystemError(task, s"${error.getClass.getSimpleName}: $message")
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
    for
      input <- testcase.input.traverse(ref => problemDataCache.loadBytes(task.problemSlug, task.problemDataVersion, ref)).map(_.getOrElse(Array.emptyByteArray))
      answerBytes <- problemDataCache.loadBytes(task.problemSlug, task.problemDataVersion, testcase.answer)
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
          IO.pure(testcaseResult(testcase, BigDecimal(0), SubmissionVerdict.TimeLimitExceeded, Some(s"Time limit exceeded on testcase ${testcase.name.value}."), runResult))
        else if runResult.exitCode.getOrElse(-1) != 0 then
          IO.pure(testcaseResult(testcase, BigDecimal(0), SubmissionVerdict.RuntimeError, Some(formatRuntimeError(testcase.name.value, command, runResult)), runResult))
        else
          scoreWithChecker(task, testcase, workingDirectory, sandbox, input, runResult.stdout, answerBytes, compiledCheckers).map {
            case Right(score) =>
              testcaseResult(
                testcase,
                score,
                if score == BigDecimal(1) then SubmissionVerdict.Accepted else SubmissionVerdict.WrongAnswer,
                Option.when(score != BigDecimal(1))(s"Wrong answer on testcase ${testcase.name.value}."),
                runResult
              )
            case Left(message) =>
              testcaseResult(testcase, BigDecimal(0), SubmissionVerdict.SystemError, Some(message), runResult)
          }
    yield result

  private def testcaseResult(
    testcase: JudgeTaskTestcase,
    score: BigDecimal,
    verdict: SubmissionVerdict,
    message: Option[String],
    runResult: ProcessResult
  ): JudgeTestcaseResult =
    JudgeTestcaseResult(testcase.name.value, clampScore(score), verdict, message, runResult.timeUsedMs, runResult.memoryUsedKb)

  private def scoreWithChecker(
    task: JudgeTask,
    testcase: JudgeTaskTestcase,
    workingDirectory: Path,
    sandbox: IsolateSandbox,
    input: Array[Byte],
    contestantOutput: String,
    answerBytes: Array[Byte],
    compiledCheckers: Map[String, RuntimeCommand]
  ): IO[Either[String, BigDecimal]] =
    testcase.checker.`type` match
      case "builtin" if testcase.checker.name.contains("exact") =>
        val expectedOutput = String(answerBytes, StandardCharsets.UTF_8)
        IO.pure(Right(if normalizeOutput(contestantOutput) == normalizeOutput(expectedOutput) then BigDecimal(1) else BigDecimal(0)))
      case "cpp" =>
        testcase.checker.source match
          case Some(source) =>
            compiledCheckers.get(source.path) match
              case None => IO.pure(Left(s"Compiled checker was not found for ${source.path}."))
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
          case None => IO.pure(Left("C++ checker source is missing."))
      case other => IO.pure(Left(s"Unsupported checker type: $other."))

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
      case Left(message) => IO.pure(Left(taskSystemError(task, message)))
      case Right(compilerPath) =>
        val sourceName = s"checker-${math.abs(sourceRef.path.hashCode)}.cpp"
        val executableName = s"checker-${math.abs(sourceRef.path.hashCode)}"
        for
          sourceBytes <- problemDataCache.loadBytes(task.problemSlug, task.problemDataVersion, sourceRef)
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
            if compileResult.timedOut then IO.pure(Left(taskSystemError(task, s"Checker compilation timed out for ${sourceRef.path}.")))
            else if compileResult.exitCode.getOrElse(-1) != 0 then IO.pure(Left(taskSystemError(task, s"Checker compilation failed for ${sourceRef.path}.\n${compileResult.stderr.trim}")))
            else ensureExecutableExists(workingDirectory.resolve(executableName)).as(Right(RuntimeCommand(s"/box/$executableName", Nil, processLimit = 1)))
        yield result
    }

  private def resolveCompilerPath(config: AppConfig): IO[Either[String, String]] =
    IO.blocking {
      resolveExecutable(config.cxx) match
        case None => Left(s"Compiler '${config.cxx}' was not found on the judger host.")
        case Some(path) if isSandboxVisibleExecutable(path) => Right(path)
        case Some(path) => Left(s"Compiler '$path' is not visible inside isolate.")
    }

  private val ScoreNumber: Regex = """(?<![\w.])(?:0(?:\.\d+)?|1(?:\.0+)?)(?![\w.])""".r

  private def parseCheckerResult(result: ProcessResult): Either[String, BigDecimal] =
    if result.timedOut then Left("Checker timed out.")
    else if result.exitCode.contains(0) then Right(BigDecimal(1))
    else
      ScoreNumber.findFirstIn(s"${result.stdout}\n${result.stderr}") match
        case Some(raw) => Right(clampScore(BigDecimal(raw)))
        case None => Left(renderDetail("Checker failed without a parseable score in [0, 1].", result, includeIsolateDetail = true))

  private def aggregateSubtask(subtask: JudgeTaskSubtask, testcases: List[JudgeTestcaseResult]): JudgeSubtaskResult =
    val score = aggregateScore(subtask.aggregation.score, testcases.map(_.score), subtask.testcases.map(_.scoreRatio))
    val verdict = aggregateVerdict(score, testcases.map(result => result.score -> result.verdict))
    val message =
      if verdict == SubmissionVerdict.Accepted then None
      else testcases.find(_.verdict != SubmissionVerdict.Accepted).flatMap(_.message)
    JudgeSubtaskResult(subtask.name, score, verdict, aggregateUsage(subtask.aggregation.time, testcases.flatMap(_.timeUsedMs)), aggregateUsage(subtask.aggregation.memory, testcases.flatMap(_.memoryUsedKb)), message, testcases)

  private def aggregateTask(task: JudgeTask, subtasks: List[JudgeSubtaskResult]): JudgeResult =
    val rawScore = aggregateScore(task.aggregation.score, subtasks.map(_.score), task.subtasks.map(_.scoreRatio))
    val score = roundFinalScore(rawScore, task.roundingScale)
    val verdict = aggregateVerdict(score, subtasks.map(result => result.score -> result.verdict))
    val resultWithoutMessage =
      JudgeResult(score, verdict, None, aggregateUsage(task.aggregation.time, subtasks.flatMap(_.timeUsedMs)), aggregateUsage(task.aggregation.memory, subtasks.flatMap(_.memoryUsedKb)), subtasks)
    resultWithoutMessage.copy(message = Some(resultMessage(resultWithoutMessage)))

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

  private def resultMessage(result: JudgeResult): String =
    if result.verdict == SubmissionVerdict.Accepted then "Accepted."
    else
      result.subtasks.iterator
        .find(_.verdict != SubmissionVerdict.Accepted)
        .flatMap(subtask => subtask.message.orElse(subtask.testcases.find(_.verdict != SubmissionVerdict.Accepted).flatMap(_.message)))
        .getOrElse(SubmissionVerdict.render(result.verdict))

  private def formatRuntimeError(testcaseName: String, command: RuntimeCommand, result: ProcessResult): String =
    val exitCode = result.exitCode.getOrElse(-1)
    val detail =
      if exitCode == 127 then
        s"Runtime command '${command.command}' was not found or was not executable inside isolate sandbox on testcase $testcaseName."
      else
        s"Runtime error on testcase $testcaseName (exit status $exitCode)."
    renderDetail(detail, result, includeIsolateDetail = true)

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
