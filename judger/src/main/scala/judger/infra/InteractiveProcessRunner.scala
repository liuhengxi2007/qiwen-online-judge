package judger.infra

import cats.effect.IO
import cats.syntax.all.*
import judgeprotocol.objects.response.{JudgeFailureReason, JudgeTaskSubtask, JudgeTaskTestcase, JudgeTaskTool}
import judger.config.AppConfig
import judger.infra.JudgeRuntimeSupport.*
import judger.objects.{RuntimeCommand, SandboxLimits, SandboxRunSpec, SandboxStdin, SandboxStdout}
import org.typelevel.log4cats.slf4j.Slf4jLogger

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path}
import java.nio.file.attribute.PosixFilePermissions
import scala.util.Try

/** 负责实际启动交互题多进程，包括 FIFO、launcher、interactor、选手和策略 provider。 */
object InteractiveProcessRunner:

  private val logger = Slf4jLogger.getLogger[IO]

  private final case class StrategyProviderRunContext(
    runtime: StrategyProviderRuntime,
    toStrategy: Path,
    fromStrategy: Path
  )

  /** 执行一次交互测试点；输出 either 中的失败代表 judger/工具系统错误。 */
  private[infra] def run(
    config: AppConfig,
    subtask: JudgeTaskSubtask,
    testcase: JudgeTaskTestcase,
    input: Array[Byte],
    workingDirectory: Path,
    sandbox: SandboxSession,
    roleCommands: Map[String, RuntimeCommand],
    interactor: JudgeTaskTool,
    interactorCommand: RuntimeCommand,
    strategyProvider: Option[StrategyProviderRuntime]
  ): IO[Either[JudgeFailureReason, InteractiveRunResult]] =
    interactor.limits match
      case None => IO.pure(Left(JudgeFailureReason.SystemError))
      case Some(interactorLimits) =>
        val safeName = s"${subtask.index}-${testcase.index}"
        val interactiveDir = workingDirectory.resolve(s"interactive-$safeName")
        val inputPath = interactiveDir.resolve("input")
        val outputPath = interactiveDir.resolve("output")
        val statusPath = interactiveDir.resolve("status")
        val participants = InteractiveJudgeRunner.interactiveParticipants(subtask.mode.roles, roleCommands, interactiveDir)
        val strategyRunContext =
          strategyProvider.map(provider => StrategyProviderRunContext(provider, interactiveDir.resolve("to-strategy"), interactiveDir.resolve("from-strategy")))
        val strategyReadMonitorPaths =
          strategyRunContext.map(context =>
            (
              context.fromStrategy,
              interactiveDir.resolve("strategy-provider-read-monitor.log"),
              context.runtime.limits.timeMs.value.toLong
            )
          )
        val fifoPaths = participants.flatMap(participant => List(participant.toParticipant, participant.fromParticipant)) ++
          strategyRunContext.toList.flatMap(context => List(context.toStrategy, context.fromStrategy))
        val sharedWallTimeLimitMs =
          InteractiveJudgeRunner.interactiveWallTimeLimitMs(
            testcase = testcase,
            roleCount = subtask.mode.roles.size,
            interactor = interactor,
            strategyProvider = strategyProvider.map(_.tool)
          )
        val participantLimits =
          SandboxLimits.runtimeWithWall(
            timeLimitMs = testcase.limits.timeMs.value.toLong,
            wallTimeLimitMs = sharedWallTimeLimitMs,
            memoryLimitMb = testcase.limits.memoryMb.value
          )
        val interactorArgs =
          interactorCommand.args ++
            List(relativeSandboxPath(workingDirectory, inputPath), relativeSandboxPath(workingDirectory, outputPath), relativeSandboxPath(workingDirectory, statusPath)) ++
            participants.flatMap { participant =>
              List(
                participant.role,
                relativeSandboxPath(workingDirectory, participant.toParticipant),
                relativeSandboxPath(workingDirectory, participant.fromParticipant)
              )
            } ++ strategyRunContext.toList.flatMap(context =>
              List("strategy", relativeSandboxPath(workingDirectory, context.toStrategy), relativeSandboxPath(workingDirectory, context.fromStrategy))
            )

        val run =
          for
            sigpipeLauncher <- ensureSigpipeIgnoreLauncher(config, workingDirectory)
            fifoRedirectLauncher <- ensureFifoRedirectLauncher(config, workingDirectory)
            readMonitorLibrary <- strategyReadMonitorPaths.traverse(_ => ensureStrategyProviderReadMonitor(config, workingDirectory))
            strategyReadMonitor =
              strategyReadMonitorPaths.zip(readMonitorLibrary).map { case ((fromStrategy, logPath, idleLimitMs), library) =>
                StrategyProviderReadMonitor(
                  librarySandboxPath = library.command,
                  targetFifoSandboxPath = relativeSandboxPath(workingDirectory, fromStrategy),
                  logPath = logPath,
                  logSandboxPath = relativeSandboxPath(workingDirectory, logPath),
                  idleLimitMs = idleLimitMs
                )
              }
            _ <- prepareInteractiveWorkspace(interactiveDir, inputPath, outputPath, statusPath, fifoPaths, input, strategyReadMonitor.toList.map(_.logPath))
            participantFibers <- participants.zipWithIndex.traverse { case (participant, index) =>
              sandbox
                .run(
                  SandboxRunSpec(
                    phase = s"participant-$safeName-${participant.occurrenceIndex}-${sanitizeInteractiveName(participant.role)}",
                    command = RuntimeCommand(
                      fifoRedirectLauncher.command,
                      fifoRedirectLauncher.args ++ List(
                        relativeSandboxPath(workingDirectory, participant.toParticipant),
                        relativeSandboxPath(workingDirectory, participant.fromParticipant),
                        participant.command.command
                      ) ++ participant.command.args,
                      participant.command.processLimit
                    ),
                    stdin = SandboxStdin.Empty,
                    stdout = SandboxStdout.Discard,
                    limits = participantLimits,
                    boxOffset = index + 1
                  ),
                  workingDirectory
                )
                .map(participant.role -> _)
                .start
            }
            strategyFiber <- strategyRunContext.traverse { context =>
              val provider = context.runtime
              sandbox
                .run(
                  SandboxRunSpec(
                    phase = s"strategy-$safeName",
                    command = RuntimeCommand(
                      fifoRedirectLauncher.command,
                      fifoRedirectLauncher.args ++ List(
                        relativeSandboxPath(workingDirectory, context.toStrategy),
                        relativeSandboxPath(workingDirectory, context.fromStrategy),
                        provider.command.command
                      ) ++ provider.command.args,
                      provider.command.processLimit
                    ),
                    stdin = SandboxStdin.Empty,
                    stdout = SandboxStdout.Discard,
                    limits = SandboxLimits.runtimeWithWall(
                      timeLimitMs = provider.limits.timeMs.value.toLong,
                      wallTimeLimitMs = sharedWallTimeLimitMs,
                      memoryLimitMb = provider.limits.memoryMb.value
                    ),
                    boxOffset = participants.size + 1
                  ),
                  workingDirectory
                )
                .start
            }
            interactorResult <- sandbox.run(
              SandboxRunSpec(
                phase = s"interactor-$safeName",
                command = RuntimeCommand(
                  sigpipeLauncher.command,
                  sigpipeLauncher.args ++ interactorLauncherArgs(strategyReadMonitor, interactorCommand.command) ++ interactorArgs,
                  interactorCommand.processLimit
                ),
                stdin = SandboxStdin.Empty,
                stdout = SandboxStdout.Capture,
                limits = SandboxLimits.runtimeWithWall(
                  timeLimitMs = interactorLimits.timeMs.value.toLong,
                  wallTimeLimitMs = sharedWallTimeLimitMs,
                  memoryLimitMb = interactorLimits.memoryMb.value
                ),
                boxOffset = participants.size + strategyProvider.fold(0)(_ => 1) + 1
              ),
              workingDirectory
            )
            participantResults <- participantFibers.traverse(joinFiber)
            strategyResult <- strategyFiber.traverse(joinFiber)
            outputAndStatus <- IO.blocking {
              val output =
                if Files.exists(outputPath) && Files.isRegularFile(outputPath) then Files.readString(outputPath, StandardCharsets.UTF_8)
                else interactorResult.stdout
              val status =
                if Files.exists(statusPath) && Files.isRegularFile(statusPath) then
                  Option(Files.readString(statusPath, StandardCharsets.UTF_8).trim).filter(_.nonEmpty)
                else None
              output -> status
            }
          yield
            val (output, status) = outputAndStatus
            Right(
              InteractiveRunResult(
                interactor = interactorResult,
                participants = participantResults,
                strategyProvider = strategyResult,
                output = output,
                status = status,
                strategyProviderReadMonitor = strategyReadMonitor
              )
            )

        run.handleErrorWith(error =>
          logger.error(error)("Interactive process run failed while preparing workspace, launchers, FIFOs, or result files.") *>
            IO.pure(Left(JudgeFailureReason.JudgerRuntimeFailed))
        )

  /** 读取策略 provider 监控日志并计算等待时间；读取失败按无监控数据处理。 */
  private[infra] def readStrategyProviderWaitMs(monitor: Option[StrategyProviderReadMonitor], interactorResult: judger.objects.ProcessResult): IO[Option[Long]] =
    monitor match
      case None => IO.pure(None)
      case Some(current) =>
        IO.blocking {
          val content =
            if Files.exists(current.logPath) && Files.isRegularFile(current.logPath) then
              Files.readString(current.logPath, StandardCharsets.UTF_8)
            else ""
          InteractiveJudgeRunner.strategyProviderReadWaitMs(content, interactorResult.wallTimeUsedMs)
        }.map(Some(_)).handleError(_ => None)

  /** 准备交互题共享目录、输入文件、状态文件和 FIFO；会设置较宽的文件权限供 isolate 内进程访问。 */
  private def prepareInteractiveWorkspace(
    interactiveDir: Path,
    inputPath: Path,
    outputPath: Path,
    statusPath: Path,
    fifoPaths: List[Path],
    input: Array[Byte],
    extraOutputPaths: List[Path] = Nil
  ): IO[Unit] =
    IO.blocking {
      Files.createDirectories(interactiveDir)
      setWorldAccessible(interactiveDir)
      Files.write(inputPath, input)
      setWorldReadable(inputPath)
      Files.deleteIfExists(outputPath)
      Files.deleteIfExists(statusPath)
      extraOutputPaths.foreach(Files.deleteIfExists)
      fifoPaths.foreach(createFifo)
    }

  /** 通过宿主机 mkfifo 创建命名管道，失败时把进程输出转换为 runtime 异常。 */
  private def createFifo(path: Path): Unit =
    Files.deleteIfExists(path)
    val process = new ProcessBuilder("mkfifo", path.toString).start()
    val exitCode = process.waitFor()
    if exitCode != 0 then
      val detail = nonEmptyOrFallback(IsolateSandbox.readStream(process.getErrorStream), IsolateSandbox.readStream(process.getInputStream), "mkfifo failed")
      throw RuntimeException(detail)
    setWorldFifo(path)

  /** 尝试把目录设置为所有参与进程可读写执行；不支持 POSIX 权限的平台忽略失败。 */
  private def setWorldAccessible(path: Path): Unit =
    Try(Files.setPosixFilePermissions(path, PosixFilePermissions.fromString("rwxrwxrwx")))
    ()

  /** 尝试把普通文件设置为所有参与进程可读写；不支持 POSIX 权限的平台忽略失败。 */
  private def setWorldReadable(path: Path): Unit =
    Try(Files.setPosixFilePermissions(path, PosixFilePermissions.fromString("rw-rw-rw-")))
    ()

  /** 尝试把 FIFO 设置为所有参与进程可读写；不支持 POSIX 权限的平台忽略失败。 */
  private def setWorldFifo(path: Path): Unit =
    Try(Files.setPosixFilePermissions(path, PosixFilePermissions.fromString("rw-rw-rw-")))
    ()

  /** 确保 SIGPIPE 忽略 launcher 已编译，并返回 sandbox 内可执行命令。 */
  private def ensureSigpipeIgnoreLauncher(config: AppConfig, workingDirectory: Path): IO[RuntimeCommand] =
    ensureCompiledLauncher(
      config = config,
      workingDirectory = workingDirectory,
      sourceName = "sigpipe-ignore.cpp",
      executableName = "sigpipe-ignore",
      source = InteractiveLauncherSources.SigpipeIgnore
    )

  /** 确保 FIFO 重定向 launcher 已编译，并返回 sandbox 内可执行命令。 */
  private def ensureFifoRedirectLauncher(config: AppConfig, workingDirectory: Path): IO[RuntimeCommand] =
    ensureCompiledLauncher(
      config = config,
      workingDirectory = workingDirectory,
      sourceName = "fifo-redirect.cpp",
      executableName = "fifo-redirect",
      source = InteractiveLauncherSources.FifoRedirect
    )

  /** 确保策略 provider 读等待监控动态库已编译，供交互器进程预加载采样。 */
  private def ensureStrategyProviderReadMonitor(config: AppConfig, workingDirectory: Path): IO[RuntimeCommand] =
    ensureCompiledLauncher(
      config = config,
      workingDirectory = workingDirectory,
      sourceName = "strategy-provider-read-monitor.cpp",
      executableName = "strategy-provider-read-monitor.so",
      source = InteractiveLauncherSources.StrategyProviderReadMonitor,
      extraCompilerArgs = List("-shared", "-fPIC", "-ldl", "-pthread"),
      requireExecutable = false
    )

  /** 组装交互器 launcher 参数；有监控库时额外传入 sandbox 内路径。 */
  private def interactorLauncherArgs(monitor: Option[StrategyProviderReadMonitor], interactorCommand: String): List[String] =
    monitor match
      case None => List(interactorCommand)
      case Some(current) =>
        List(
          "--strategy-provider-read-monitor",
          current.librarySandboxPath,
          current.targetFifoSandboxPath,
          current.logSandboxPath,
          interactorCommand
        )

  /** 编译或复用交互辅助 launcher；编译失败会作为 judger runtime 异常抛出。 */
  private def ensureCompiledLauncher(
    config: AppConfig,
    workingDirectory: Path,
    sourceName: String,
    executableName: String,
    source: String,
    extraCompilerArgs: List[String] = Nil,
    requireExecutable: Boolean = true
  ): IO[RuntimeCommand] =
    val executablePath = workingDirectory.resolve(executableName)
    if Files.exists(executablePath) && Files.isRegularFile(executablePath) && (!requireExecutable || Files.isExecutable(executablePath)) then
      IO.pure(RuntimeCommand(s"/box/$executableName", Nil, processLimit = 1))
    else
      JudgeToolPreparation.resolveCompilerPath(config).flatMap {
        case Left(message) => IO.raiseError(RuntimeException(message))
        case Right(compilerPath) =>
          for
            _ <- IO.blocking(Files.writeString(workingDirectory.resolve(sourceName), source, StandardCharsets.UTF_8))
            compileResult <- runHostProcess(
              command = compilerPath,
              args = List(sourceName, "-o", executableName, "-O2") ++ extraCompilerArgs,
              cwd = workingDirectory,
              stdin = None,
              limits = SandboxLimits.runtime(timeLimitMs = 15000L, memoryLimitMb = 2048),
              stdoutName = s".$executableName.compile.stdout",
              stderrName = s".$executableName.compile.stderr"
            )
            _ <-
              if compileResult.timedOut || compileResult.exitCode.getOrElse(-1) != 0 then
                IO.raiseError(RuntimeException(s"Failed to compile $executableName launcher."))
              else if requireExecutable then ensureExecutableExists(executablePath)
              else ensureRegularFileExists(executablePath)
          yield RuntimeCommand(s"/box/$executableName", Nil, processLimit = 1)
      }

  /** 校验编译产物是普通文件，并调整权限供 sandbox 内进程读取。 */
  private def ensureRegularFileExists(path: Path): IO[Unit] =
    IO.blocking {
      if !Files.exists(path) then
        throw RuntimeException(s"Prepared file was not produced at ${path.toAbsolutePath}.")
      if !Files.isRegularFile(path) then
        throw RuntimeException(s"Prepared path is not a regular file: ${path.toAbsolutePath}.")
      setWorldReadable(path)
    }

  /** 把宿主机工作目录内路径转换为交互 launcher 使用的相对 sandbox 路径。 */
  private def relativeSandboxPath(workingDirectory: Path, path: Path): String =
    workingDirectory.relativize(path).toString

  /** 将 role 名称规整为可用于临时文件和 FIFO 名称的安全片段。 */
  private def sanitizeInteractiveName(value: String): String =
    value.map {
      case current if current.isLetterOrDigit || current == '-' || current == '_' => current
      case _ => '_'
    }

  /** 等待并传播子 fiber 的结果、错误或取消状态。 */
  private def joinFiber[A](fiber: cats.effect.FiberIO[A]): IO[A] =
    fiber.join.flatMap {
      case cats.effect.Outcome.Succeeded(result) => result
      case cats.effect.Outcome.Errored(error) => IO.raiseError(error)
      case cats.effect.Outcome.Canceled() => IO.canceled *> IO.never[A]
    }
