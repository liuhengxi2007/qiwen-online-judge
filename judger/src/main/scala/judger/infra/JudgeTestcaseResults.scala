package judger.infra

import judgeprotocol.objects.SubmissionVerdict
import judgeprotocol.objects.response.{JudgeFailureReason, JudgeResultSummary, JudgeSubtaskResult, JudgeTaskSubtask, JudgeTaskTestcase, JudgeTestcaseResult}
import judger.objects.ProcessResult

/** 构造测试点和子任务结果的集中工具，保证失败节点形状一致。 */
object JudgeTestcaseResults:
  /** 从一次运行结果构造测试点结果，并钳制分数到协议允许范围。 */
  private[infra] def testcaseResult(
    testcase: JudgeTaskTestcase,
    score: BigDecimal,
    verdict: SubmissionVerdict,
    message: Option[String],
    reason: Option[JudgeFailureReason],
    runResult: ProcessResult
  ): JudgeTestcaseResult =
    JudgeTestcaseResult(
      testcase.index,
      testcase.label,
      testcase.testcaseType,
      JudgeResultAggregator.clampScore(score),
      verdict,
      message,
      reason,
      runResult.timeUsedMs,
      runResult.memoryUsedKb
    )

  /** 构造单测试点系统错误结果。 */
  private[infra] def testcaseSystemError(testcase: JudgeTaskTestcase, reason: JudgeFailureReason): JudgeTestcaseResult =
    JudgeTestcaseResult(testcase.index, testcase.label, testcase.testcaseType, BigDecimal(0), SubmissionVerdict.SystemError, None, Some(reason), None, None)

  /** 构造单测试点编译错误结果。 */
  private[infra] def testcaseCompileError(testcase: JudgeTaskTestcase): JudgeTestcaseResult =
    JudgeTestcaseResult(testcase.index, testcase.label, testcase.testcaseType, BigDecimal(0), SubmissionVerdict.CompileError, None, None, None, None)

  /** 构造协议层接受的测试点结果，表示策略 provider 缺失或工具超限时不惩罚提交。 */
  private[infra] def testcaseAcceptedByProtocol(testcase: JudgeTaskTestcase): JudgeTestcaseResult =
    JudgeTestcaseResult(testcase.index, testcase.label, testcase.testcaseType, BigDecimal(1), SubmissionVerdict.AcceptedByProtocol, None, None, Some(0L), Some(0L))

  /** 构造整子任务编译错误结果，通常用于交互题某个 role 无法准备。 */
  private[infra] def subtaskCompileError(subtask: JudgeTaskSubtask): JudgeSubtaskResult =
    val summary = JudgeResultSummary.nonSystem(BigDecimal(0), SubmissionVerdict.CompileError, None, None)
    JudgeSubtaskResult(
      index = subtask.index,
      label = subtask.label,
      baseResult = summary,
      worstResult = summary,
      testcases = subtask.testcases.map(testcaseCompileError)
    )

  /** 构造整子任务系统错误结果，并把错误原因复制到每个测试点。 */
  private[infra] def subtaskSystemError(subtask: JudgeTaskSubtask, reason: JudgeFailureReason): JudgeSubtaskResult =
    val summary = JudgeResultSummary.failed(reason)
    JudgeSubtaskResult(
      index = subtask.index,
      label = subtask.label,
      baseResult = summary,
      worstResult = summary,
      testcases = subtask.testcases.map(testcase => testcaseSystemError(testcase, reason))
    )

  /** 把 Text 语言输出包装成一次成功运行，便于复用 checker 流程。 */
  private[infra] def successfulTextRun(output: String): ProcessResult =
    ProcessResult(
      exitCode = Some(0),
      isolateStatus = None,
      isolateMessage = None,
      stdout = output,
      stderr = "",
      timedOut = false,
      timeUsedMs = Some(0L),
      wallTimeUsedMs = Some(0L),
      memoryUsedKb = Some(0L)
    )
