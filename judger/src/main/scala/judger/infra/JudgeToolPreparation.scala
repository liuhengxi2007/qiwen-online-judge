package judger.infra

import cats.effect.IO
import cats.syntax.all.*
import judgeprotocol.objects.{SubmissionLanguage, SubmissionSourceCode}
import judgeprotocol.objects.request.ReportJudgeResultRequest
import judgeprotocol.objects.response.*
import judger.config.AppConfig
import judger.infra.JudgeRuntimeSupport.*
import judger.objects.{RuntimeCommand, SandboxLimits}

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path}

/** 准备提交程序和题目工具，统一处理源码下载、编译和失败上报。 */
object JudgeToolPreparation:
  /** 已准备好的提交程序集合，区分可执行命令、编译失败 role 和 Text 语言输出。 */
  private[judger] final case class PreparedPrograms(
    commands: Map[String, RuntimeCommand],
    compileFailedRoles: Set[String],
    textOutputs: Map[String, String]
  )

  /** 已准备好的 checker/validator/interactor/strategy provider 命令集合。 */
  private[infra] final case class PreparedTools(
    validators: Map[JudgeTaskFilePath, RuntimeCommand],
    checkers: Map[JudgeTaskFilePath, RuntimeCommand],
    interactors: Map[JudgeTaskFilePath, RuntimeCommand],
    strategyProviders: Map[JudgeTaskFilePath, RuntimeCommand],
    failedStrategyProviders: Set[JudgeTaskFilePath]
  )

  /** C++ 工具编译结果，区分成功、用户工具编译失败和 judger 系统失败。 */
  private[infra] enum ToolCompileOutcome:
    case Success(command: RuntimeCommand)
    case CompileFailed
    case SystemFailed(reason: JudgeFailureReason)

  /** 准备提交中的每个 role；任一系统错误会直接返回整题失败回报。 */
  private[infra] def preparePrograms(
    task: JudgeTask,
    config: AppConfig,
    workingDirectory: Path,
    problemDataCache: ProblemDataCache,
    runtimes: Map[SubmissionLanguage, JudgeRuntime]
  ): IO[Either[ReportJudgeResultRequest, PreparedPrograms]] =
    task.programs.toList
      .traverse { case (role, program) =>
        program.language match
          case SubmissionLanguage.Text =>
            IO.pure(Right(role -> (None, Some(program.sourceCode.value), false)))
          case _ =>
            runtimes.get(program.language) match
              case None =>
                IO.pure(Right(role -> (None, None, true)))
              case Some(runtime) =>
                loadProgramStub(task, program, problemDataCache).flatMap {
                  case Left(result) => IO.pure(Left(result))
                  case Right(stubSourceCode) =>
                    loadProgramHeaders(task, program, problemDataCache).flatMap {
                      case Left(result) => IO.pure(Left(result))
                      case Right(headers) =>
                        runtime.prepare(role, program.sourceCode, stubSourceCode, headers, config, workingDirectory).map {
                          case Right(command) => Right(role -> (Some(command), None, false))
                          case Left(ProgramPrepareFailure.CompileError) => Right(role -> (None, None, true))
                          case Left(ProgramPrepareFailure.SystemError(reason)) => Left(taskSystemError(task, reason))
                        }
                    }
                }
      }
      .map { prepared =>
        prepared.collectFirst { case Left(result) => Left(result) }.getOrElse {
          val roleCommands = prepared.collect { case Right((role, (Some(command), _, _))) => role -> command }.toMap
          val textOutputs = prepared.collect { case Right((role, (_, Some(output), _))) => role -> output }.toMap
          val failedRoles = prepared.collect { case Right((role, (None, None, true))) => role }.toSet
          Right(PreparedPrograms(roleCommands, failedRoles, textOutputs))
        }
      }

  private def loadProgramStub(
    task: JudgeTask,
    program: JudgeTaskProgram,
    problemDataCache: ProblemDataCache
  ): IO[Either[ReportJudgeResultRequest, Option[SubmissionSourceCode]]] =
    program.stub match
      case None => IO.pure(Right(None))
      case Some(stub) =>
        problemDataCache.loadBytes(task.problemSlug, task.problemDataVersion, stub).attempt.map {
          case Left(_) => Left(taskSystemError(task, JudgeFailureReason.ProblemDataLoadFailed))
          case Right(bytes) => Right(Some(SubmissionSourceCode(new String(bytes, StandardCharsets.UTF_8))))
        }

  private def loadProgramHeaders(
    task: JudgeTask,
    program: JudgeTaskProgram,
    problemDataCache: ProblemDataCache
  ): IO[Either[ReportJudgeResultRequest, List[ProgramHeaderSource]]] =
    program.headers.traverse { header =>
      problemDataCache.loadBytes(task.problemSlug, task.problemDataVersion, header).attempt.map {
        case Left(_) => Left(taskSystemError(task, JudgeFailureReason.ProblemDataLoadFailed))
        case Right(bytes) =>
          Right(ProgramHeaderSource(headerIncludeName(header), SubmissionSourceCode(new String(bytes, StandardCharsets.UTF_8))))
      }
    }.map { loaded =>
      loaded.collectFirst { case Left(result) => Left(result) }.getOrElse(Right(loaded.collect { case Right(header) => header }))
    }

  private def headerIncludeName(header: JudgeTaskFileRef): String =
    header.path.value.split('/').lastOption.getOrElse(header.path.value)

  /** 准备任务中引用的题目工具；必需工具失败会转为系统失败，策略 provider 编译失败可被业务接受。 */
  private[infra] def prepareTools(
    task: JudgeTask,
    config: AppConfig,
    workingDirectory: Path,
    problemDataCache: ProblemDataCache
  ): IO[Either[ReportJudgeResultRequest, PreparedTools]] =
    val validatorSources = uniqueRefs(task.subtasks.flatMap(_.validator.map(_.source)))
    val checkerSources = uniqueRefs(task.subtasks.flatMap(_.testcases).flatMap(_.checker.source))
    val interactorSources = uniqueRefs(task.subtasks.flatMap(_.mode.interactor.map(_.source)))
    val strategyProviderSources = uniqueRefs(task.subtasks.flatMap(_.testcases).flatMap(_.strategyProvider.map(_.source)))

    for
      validators <- compileRequiredTools(task, config, workingDirectory, problemDataCache, validatorSources, JudgeFailureReason.CheckerCompileFailed)
      checkers <- validators match
        case Left(result) => IO.pure(Left(result))
        case Right(_) => compileRequiredTools(task, config, workingDirectory, problemDataCache, checkerSources, JudgeFailureReason.CheckerCompileFailed)
      interactors <- checkers match
        case Left(result) => IO.pure(Left(result))
        case Right(_) => compileRequiredTools(task, config, workingDirectory, problemDataCache, interactorSources, JudgeFailureReason.InteractorCompileFailed)
      strategyProviders <- interactors match
        case Left(result) => IO.pure(Left(result))
        case Right(_) => compileStrategyProviders(task, config, workingDirectory, problemDataCache, strategyProviderSources)
    yield
      for
        validatorMap <- validators
        checkerMap <- checkers
        interactorMap <- interactors
        strategyTuple <- strategyProviders
      yield
        val (strategyCommands, failedStrategies) = strategyTuple
        PreparedTools(
          validators = validatorMap,
          checkers = checkerMap,
          interactors = interactorMap,
          strategyProviders = strategyCommands,
          failedStrategyProviders = failedStrategies
        )

  private def compileRequiredTools(
    task: JudgeTask,
    config: AppConfig,
    workingDirectory: Path,
    problemDataCache: ProblemDataCache,
    sources: List[JudgeTaskFileRef],
    compileFailureReason: JudgeFailureReason
  ): IO[Either[ReportJudgeResultRequest, Map[JudgeTaskFilePath, RuntimeCommand]]] =
    sources.traverse { source =>
      compileCppTool(task, config, workingDirectory, problemDataCache, source).map {
        case ToolCompileOutcome.Success(command) => Right(source.path -> command)
        case ToolCompileOutcome.CompileFailed => Left(taskSystemError(task, compileFailureReason))
        case ToolCompileOutcome.SystemFailed(reason) => Left(taskSystemError(task, reason))
      }
    }.map { compiled =>
      compiled.collectFirst { case Left(result) => Left(result) }.getOrElse(Right(compiled.collect { case Right(entry) => entry }.toMap))
    }

  private def compileStrategyProviders(
    task: JudgeTask,
    config: AppConfig,
    workingDirectory: Path,
    problemDataCache: ProblemDataCache,
    sources: List[JudgeTaskFileRef]
  ): IO[Either[ReportJudgeResultRequest, (Map[JudgeTaskFilePath, RuntimeCommand], Set[JudgeTaskFilePath])]] =
    sources.traverse { source =>
      compileCppTool(task, config, workingDirectory, problemDataCache, source).map {
        case ToolCompileOutcome.Success(command) => Right(source.path -> Some(command))
        case ToolCompileOutcome.CompileFailed => Right(source.path -> None)
        case ToolCompileOutcome.SystemFailed(reason) => Left(taskSystemError(task, reason))
      }
    }.map { compiled =>
      compiled.collectFirst { case Left(result) => Left(result) }.getOrElse {
        val commands = compiled.collect { case Right((path, Some(command))) => path -> command }.toMap
        val failed = compiled.collect { case Right((path, None)) => path }.toSet
        Right(commands -> failed)
      }
    }

  private def compileCppTool(
    task: JudgeTask,
    config: AppConfig,
    workingDirectory: Path,
    problemDataCache: ProblemDataCache,
    source: JudgeTaskFileRef
  ): IO[ToolCompileOutcome] =
    problemDataCache.loadBytes(task.problemSlug, task.problemDataVersion, source).attempt.flatMap {
      case Left(_) => IO.pure(ToolCompileOutcome.SystemFailed(JudgeFailureReason.ProblemDataLoadFailed))
      case Right(bytes) => compileCppToolBytes(task, config, workingDirectory, source.path.value, bytes)
    }

  /** 编译一段 C++ 工具源码字节，产物作为 /box 下的可执行命令返回。 */
  private[infra] def compileCppToolBytes(
    task: JudgeTask,
    config: AppConfig,
    workingDirectory: Path,
    sourceNameHint: String,
    sourceBytes: Array[Byte]
  ): IO[ToolCompileOutcome] =
    // 注意：task 参数保留给调用方语义和未来错误上下文，当前字节级编译逻辑只需要 config 与工作目录。
    val _ = task
    resolveCompilerPath(config).flatMap {
      // FIXME-CN: 编译器不可见属于 judger 环境问题，这里被折叠成工具 CompileFailed，可能把系统故障误报为 checker/strategy 编译失败。
      case Left(_) => IO.pure(ToolCompileOutcome.CompileFailed)
      case Right(compilerPath) =>
        // FIXME-CN: math.abs(Int.MinValue) 仍为负数，极端 hash 碰撞/负值会进入文件名；应使用无符号或 sha256 片段命名。
        val safeHash = math.abs(sourceNameHint.hashCode)
        val sourceName = s"tool-$safeHash.cpp"
        val executableName = s"tool-$safeHash"
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
            if compileResult.timedOut || compileResult.exitCode.getOrElse(-1) != 0 then IO.pure(ToolCompileOutcome.CompileFailed)
            else
              ensureExecutableExists(workingDirectory.resolve(executableName)).attempt.map {
                case Right(_) => ToolCompileOutcome.Success(RuntimeCommand(s"/box/$executableName", Nil, processLimit = 1))
                case Left(_) => ToolCompileOutcome.CompileFailed
              }
        yield result
    }

  /** 解析 C++ 编译器路径，并要求其在 isolate 默认挂载中可见。 */
  private[infra] def resolveCompilerPath(config: AppConfig): IO[Either[String, String]] =
    IO.blocking {
      resolveExecutable(config.cxx) match
        case None => Left(s"Compiler '${config.cxx}' was not found on the judger host.")
        case Some(path) if isSandboxVisibleExecutable(path) => Right(path)
        case Some(path) => Left(s"Compiler '$path' is not visible inside isolate.")
    }

  private def uniqueRefs(refs: List[JudgeTaskFileRef]): List[JudgeTaskFileRef] =
    refs.groupBy(_.path).values.map(_.head).toList

  // FIXME-CN: MinimalTestlibHeader 只实现了 testlib 的很小子集，使用高级 API 的 checker/validator 会编译或运行失败。
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
      |  if (result == _ok) {
      |    cout << "1";
      |    exit(0);
      |  }
      |  if (result == _wa || result == _pe) {
      |    cout << "0";
      |    exit(0);
      |  }
      |  exit(3);
      |}
      |
      |inline void quitp(double score, const char* format = "", ...) {
      |  cout << max(0.0, min(1.0, score));
      |  exit(0);
      |}
      |""".stripMargin
