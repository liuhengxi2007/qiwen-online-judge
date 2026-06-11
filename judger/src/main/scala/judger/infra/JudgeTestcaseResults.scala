package judger.infra

import judgeprotocol.objects.SubmissionVerdict
import judgeprotocol.objects.response.{JudgeFailureReason, JudgeResultSummary, JudgeSubtaskResult, JudgeTaskSubtask, JudgeTaskTestcase, JudgeTestcaseResult}
import judger.objects.ProcessResult

object JudgeTestcaseResults:
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

  private[infra] def testcaseSystemError(testcase: JudgeTaskTestcase, reason: JudgeFailureReason): JudgeTestcaseResult =
    JudgeTestcaseResult(testcase.index, testcase.label, testcase.testcaseType, BigDecimal(0), SubmissionVerdict.SystemError, None, Some(reason), None, None)

  private[infra] def testcaseCompileError(testcase: JudgeTaskTestcase): JudgeTestcaseResult =
    JudgeTestcaseResult(testcase.index, testcase.label, testcase.testcaseType, BigDecimal(0), SubmissionVerdict.CompileError, None, None, None, None)

  private[infra] def testcaseAcceptedByProtocol(testcase: JudgeTaskTestcase): JudgeTestcaseResult =
    JudgeTestcaseResult(testcase.index, testcase.label, testcase.testcaseType, BigDecimal(1), SubmissionVerdict.AcceptedByProtocol, None, None, Some(0L), Some(0L))

  private[infra] def subtaskCompileError(subtask: JudgeTaskSubtask): JudgeSubtaskResult =
    val summary = JudgeResultSummary.nonSystem(BigDecimal(0), SubmissionVerdict.CompileError, None, None)
    JudgeSubtaskResult(
      index = subtask.index,
      label = subtask.label,
      baseResult = summary,
      worstResult = summary,
      testcases = subtask.testcases.map(testcaseCompileError)
    )

  private[infra] def subtaskSystemError(subtask: JudgeTaskSubtask, reason: JudgeFailureReason): JudgeSubtaskResult =
    val summary = JudgeResultSummary.failed(reason)
    JudgeSubtaskResult(
      index = subtask.index,
      label = subtask.label,
      baseResult = summary,
      worstResult = summary,
      testcases = subtask.testcases.map(testcase => testcaseSystemError(testcase, reason))
    )

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
