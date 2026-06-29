package judger.infra

import judgeprotocol.objects.SubmissionVerdict
import judgeprotocol.objects.response.{JudgeTaskTestcase, JudgeTaskTool}
import judger.objects.{ProcessResult, RuntimeCommand}

import java.nio.file.Path

/** 交互题判定辅助逻辑，负责参与者管道布局、共享时间限制和超时归因。 */
object InteractiveJudgeRunner:
  /** 一个交互参与程序的命令和双向 FIFO 路径。 */
  private[judger] final case class InteractiveParticipant(
    role: String,
    occurrenceIndex: Int,
    command: RuntimeCommand,
    toParticipant: Path,
    fromParticipant: Path
  )

  /** 根据 roles 和已准备命令生成参与者列表；缺失 role 会被过滤并由上层处理。 */
  private[judger] def interactiveParticipants(
    roles: List[String],
    roleCommands: Map[String, RuntimeCommand],
    interactiveDir: Path
  ): List[InteractiveParticipant] =
    roles.zipWithIndex.flatMap { case (role, index) =>
      roleCommands.get(role).map { command =>
        val safeRole = sanitizeInteractiveName(role)
        val occurrenceIndex = index + 1
        InteractiveParticipant(
          role = role,
          occurrenceIndex = occurrenceIndex,
          command = command,
          toParticipant = interactiveDir.resolve(s"to-participant-$occurrenceIndex-$safeRole"),
          fromParticipant = interactiveDir.resolve(s"from-participant-$occurrenceIndex-$safeRole")
        )
      }
    }

  /** 计算交互题所有进程共享的墙钟限制，用于覆盖通信等待时间。 */
  private[judger] def interactiveWallTimeLimitMs(
    testcase: JudgeTaskTestcase,
    roleCount: Int,
    interactor: JudgeTaskTool,
    strategyProvider: Option[JudgeTaskTool]
  ): Long =
    val totalCpuBudgetMs =
      testcase.limits.timeMs.value.toLong * math.max(0, roleCount).toLong +
        interactor.limits.map(_.timeMs.value.toLong).getOrElse(0L) +
        strategyProvider.flatMap(_.limits).map(_.timeMs.value.toLong).getOrElse(0L)
    (totalCpuBudgetMs * 3L + 1L) / 2L + 500L

  /** 判断 interactor 或策略 provider 是否真正耗尽自身 CPU 时间。 */
  private[judger] def interactiveToolCpuLimitExceeded(
    interactor: JudgeTaskTool,
    strategyProvider: Option[JudgeTaskTool],
    interactorResult: ProcessResult,
    strategyResult: Option[ProcessResult]
  ): Boolean =
    toolCpuLimitExceeded(interactor, interactorResult) ||
      strategyProvider.exists(provider => strategyResult.exists(result => toolCpuLimitExceeded(provider, result)))

  /** 根据工具配置判断单个工具进程是否 CPU 超限。 */
  private[judger] def toolCpuLimitExceeded(tool: JudgeTaskTool, result: ProcessResult): Boolean =
    tool.limits.exists(limits => cpuLimitExceeded(result, limits.timeMs.value.toLong))

  /** 判断某进程是否超时且 CPU 用量达到给定限制。 */
  private[judger] def cpuLimitExceeded(result: ProcessResult, timeLimitMs: Long): Boolean =
    result.timedOut && result.timeUsedMs.exists(_ >= timeLimitMs)

  /** 对只有墙钟超时而无 CPU 超限的交互运行做归因，区分选手失败、策略等待和 idleness。 */
  private[judger] def interactiveWallOnlyVerdict(
    participants: List[(String, ProcessResult)],
    participantCpuLimitMs: Long,
    processes: List[(ProcessResult, Long)],
    fallback: ProcessResult,
    strategyProviderReadWaitMs: Option[Long] = None,
    strategyProviderIdleLimitMs: Option[Long] = None
  ): Option[(SubmissionVerdict, ProcessResult)] =
    Option.when(interactiveWallOnlyTimeout(processes)) {
      participantFailure(participants, participantCpuLimitMs).getOrElse {
        if strategyProviderIdleLimitMs.exists(limit => strategyProviderReadWaitMs.exists(_ >= limit)) then
          SubmissionVerdict.AcceptedByProtocol -> fallback
        else SubmissionVerdict.IdlenessLimitExceeded -> fallback
      }
    }

  /** 判断是否存在墙钟超时但没有任何进程达到自身 CPU 限制。 */
  private[judger] def interactiveWallOnlyTimeout(processes: List[(ProcessResult, Long)]): Boolean =
    processes.exists { case (result, _) => result.timedOut } &&
      !processes.exists { case (result, timeLimitMs) => cpuLimitExceeded(result, timeLimitMs) }

  /** 从 LD_PRELOAD 监控日志计算 interactor 读取策略 provider FIFO 的累计等待时间。 */
  private[judger] def strategyProviderReadWaitMs(logContent: String, interactorWallTimeUsedMs: Option[Long]): Long =
    final case class Begin(seq: Long, timestampMs: Long)
    val parsedEvents =
      logContent.linesIterator.toList.flatMap { line =>
        line.trim.split("\\s+").toList match
          case "begin" :: rawSeq :: rawTimestamp :: Nil =>
            for
              seq <- rawSeq.toLongOption
              timestamp <- rawTimestamp.toLongOption
            yield Left(Begin(seq, timestamp))
          case "end" :: rawSeq :: rawTimestamp :: _ =>
            for
              seq <- rawSeq.toLongOption
              timestamp <- rawTimestamp.toLongOption
            yield Right(seq -> timestamp)
          case _ => None
      }

    val firstTimestamp =
      parsedEvents.flatMap {
        case Left(begin) => Some(begin.timestampMs)
        case Right((_, timestamp)) => Some(timestamp)
      }.minOption
    val fallbackEndMs =
      for
        first <- firstTimestamp
        wall <- interactorWallTimeUsedMs
      yield first + math.max(0L, wall)

    val (completeWaitMs, pendingBegins) =
      parsedEvents.foldLeft(0L -> Map.empty[Long, Long]) {
        case ((total, pending), Left(begin)) =>
          total -> pending.updated(begin.seq, begin.timestampMs)
        case ((total, pending), Right((seq, endMs))) =>
          pending.get(seq) match
            case Some(beginMs) => (total + math.max(0L, endMs - beginMs)) -> pending.removed(seq)
            case None => total -> pending
      }
    val pendingWaitMs =
      fallbackEndMs.toList.flatMap(endMs => pendingBegins.values.map(beginMs => math.max(0L, endMs - beginMs))).sum
    completeWaitMs + pendingWaitMs

  /** 找到首个选手进程的 TLE 或 RE，用于交互题失败归因。 */
  private[judger] def participantFailure(participants: List[(String, ProcessResult)], timeLimitMs: Long): Option[(SubmissionVerdict, ProcessResult)] =
    participants.collectFirst {
      case (_, result) if cpuLimitExceeded(result, timeLimitMs) => SubmissionVerdict.TimeLimitExceeded -> result
      case (_, result) if result.exitCode.getOrElse(-1) != 0 => SubmissionVerdict.RuntimeError -> result
    }

  private def sanitizeInteractiveName(value: String): String =
    value.map {
      case current if current.isLetterOrDigit || current == '-' || current == '_' => current
      case _ => '_'
    }
