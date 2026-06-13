package judger.infra

import judgeprotocol.objects.response.JudgeTaskTool
import judger.objects.{ProcessResult, RuntimeCommand}

import java.nio.file.Path

/** 已编译的策略 provider 运行时信息，包含原工具配置和 sandbox 命令。 */
private[infra] final case class StrategyProviderRuntime(
  tool: JudgeTaskTool,
  command: RuntimeCommand
)

/** 一次交互题联合运行的结果，汇总 interactor、选手进程、策略 provider、输出和状态文件。 */
private[infra] final case class InteractiveRunResult(
  interactor: ProcessResult,
  participants: List[(String, ProcessResult)],
  strategyProvider: Option[ProcessResult],
  output: String,
  status: Option[String],
  strategyProviderReadMonitor: Option[StrategyProviderReadMonitor]
)

/** 策略 provider 读监控配置，用于判断 interactor 是否因等待策略 provider 输入而墙钟超时。 */
private[infra] final case class StrategyProviderReadMonitor(
  librarySandboxPath: String,
  targetFifoSandboxPath: String,
  logPath: Path,
  logSandboxPath: String,
  idleLimitMs: Long
)
