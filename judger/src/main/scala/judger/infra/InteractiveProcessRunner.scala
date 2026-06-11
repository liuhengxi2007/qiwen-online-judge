package judger.infra

import cats.effect.IO
import cats.syntax.all.*
import judgeprotocol.objects.response.{JudgeFailureReason, JudgeTaskSubtask, JudgeTaskTestcase, JudgeTaskTool}
import judger.config.AppConfig
import judger.infra.JudgeRuntimeSupport.*
import judger.objects.{RuntimeCommand, SandboxLimits, SandboxRunSpec, SandboxStdin, SandboxStdout}

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path}
import java.nio.file.attribute.PosixFilePermissions
import scala.util.Try

object InteractiveProcessRunner:
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
        val strategyFifos = strategyProvider.map(_ => interactiveDir.resolve("to-strategy") -> interactiveDir.resolve("from-strategy"))
        val strategyReadMonitorPaths =
          strategyProvider.flatMap { provider =>
            strategyFifos.map { case (_, fromStrategy) =>
              (
                fromStrategy,
                interactiveDir.resolve("strategy-provider-read-monitor.log"),
                provider.tool.limits.map(_.timeMs.value.toLong).getOrElse(0L)
              )
            }
          }
        val fifoPaths = participants.flatMap(participant => List(participant.toParticipant, participant.fromParticipant)) ++
          strategyFifos.toList.flatMap { case (toStrategy, fromStrategy) => List(toStrategy, fromStrategy) }
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
            } ++
            strategyFifos.toList.flatMap { case (toStrategy, fromStrategy) =>
              List("strategy", relativeSandboxPath(workingDirectory, toStrategy), relativeSandboxPath(workingDirectory, fromStrategy))
            }

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
            strategyFiber <- strategyProvider.traverse { provider =>
              val (toStrategy, fromStrategy) = strategyFifos.get
              val limits = provider.tool.limits.get
              sandbox
                .run(
                  SandboxRunSpec(
                    phase = s"strategy-$safeName",
                    command = RuntimeCommand(
                      fifoRedirectLauncher.command,
                      fifoRedirectLauncher.args ++ List(
                        relativeSandboxPath(workingDirectory, toStrategy),
                        relativeSandboxPath(workingDirectory, fromStrategy),
                        provider.command.command
                      ) ++ provider.command.args,
                      provider.command.processLimit
                    ),
                    stdin = SandboxStdin.Empty,
                    stdout = SandboxStdout.Discard,
                    limits = SandboxLimits.runtimeWithWall(
                      timeLimitMs = limits.timeMs.value.toLong,
                      wallTimeLimitMs = sharedWallTimeLimitMs,
                      memoryLimitMb = limits.memoryMb.value
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

        run.handleError(_ => Left(JudgeFailureReason.JudgerRuntimeFailed))

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

  private def createFifo(path: Path): Unit =
    Files.deleteIfExists(path)
    val process = new ProcessBuilder("mkfifo", path.toString).start()
    val exitCode = process.waitFor()
    if exitCode != 0 then
      val detail = nonEmptyOrFallback(IsolateSandbox.readStream(process.getErrorStream), IsolateSandbox.readStream(process.getInputStream), "mkfifo failed")
      throw RuntimeException(detail)
    setWorldFifo(path)

  private def setWorldAccessible(path: Path): Unit =
    Try(Files.setPosixFilePermissions(path, PosixFilePermissions.fromString("rwxrwxrwx")))
    ()

  private def setWorldReadable(path: Path): Unit =
    Try(Files.setPosixFilePermissions(path, PosixFilePermissions.fromString("rw-rw-rw-")))
    ()

  private def setWorldFifo(path: Path): Unit =
    Try(Files.setPosixFilePermissions(path, PosixFilePermissions.fromString("rw-rw-rw-")))
    ()

  private def ensureSigpipeIgnoreLauncher(config: AppConfig, workingDirectory: Path): IO[RuntimeCommand] =
    ensureCompiledLauncher(
      config = config,
      workingDirectory = workingDirectory,
      sourceName = "sigpipe-ignore.cpp",
      executableName = "sigpipe-ignore",
      source = InteractiveLauncherSources.SigpipeIgnore
    )

  private def ensureFifoRedirectLauncher(config: AppConfig, workingDirectory: Path): IO[RuntimeCommand] =
    ensureCompiledLauncher(
      config = config,
      workingDirectory = workingDirectory,
      sourceName = "fifo-redirect.cpp",
      executableName = "fifo-redirect",
      source = InteractiveLauncherSources.FifoRedirect
    )

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

  private def ensureRegularFileExists(path: Path): IO[Unit] =
    IO.blocking {
      if !Files.exists(path) then
        throw RuntimeException(s"Prepared file was not produced at ${path.toAbsolutePath}.")
      if !Files.isRegularFile(path) then
        throw RuntimeException(s"Prepared path is not a regular file: ${path.toAbsolutePath}.")
      setWorldReadable(path)
    }

  private def relativeSandboxPath(workingDirectory: Path, path: Path): String =
    workingDirectory.relativize(path).toString

  private def sanitizeInteractiveName(value: String): String =
    value.map {
      case current if current.isLetterOrDigit || current == '-' || current == '_' => current
      case _ => '_'
    }

  private def joinFiber[A](fiber: cats.effect.FiberIO[A]): IO[A] =
    fiber.join.flatMap {
      case cats.effect.Outcome.Succeeded(result) => result
      case cats.effect.Outcome.Errored(error) => IO.raiseError(error)
      case cats.effect.Outcome.Canceled() => IO.canceled *> IO.never[A]
    }
