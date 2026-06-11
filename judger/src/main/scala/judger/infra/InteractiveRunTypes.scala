package judger.infra

import judgeprotocol.objects.response.JudgeTaskTool
import judger.objects.{ProcessResult, RuntimeCommand}

import java.nio.file.Path

private[infra] final case class StrategyProviderRuntime(
  tool: JudgeTaskTool,
  command: RuntimeCommand
)

private[infra] final case class InteractiveRunResult(
  interactor: ProcessResult,
  participants: List[(String, ProcessResult)],
  strategyProvider: Option[ProcessResult],
  output: String,
  status: Option[String],
  strategyProviderReadMonitor: Option[StrategyProviderReadMonitor]
)

private[infra] final case class StrategyProviderReadMonitor(
  librarySandboxPath: String,
  targetFifoSandboxPath: String,
  logPath: Path,
  logSandboxPath: String,
  idleLimitMs: Long
)
