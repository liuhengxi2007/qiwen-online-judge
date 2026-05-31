package domains.submission.utils

import domains.submission.objects.SubmissionStatus
import domains.submission.objects.internal.{SubmissionJudgeCompletion, SubmissionJudgeState}
import judgeprotocol.objects.SubmissionVerdict
import judgeprotocol.objects.response.JudgeResult
import munit.FunSuite

import java.time.Instant

class SubmissionJudgeRulesSuite extends FunSuite:

  private val startedAt = Instant.parse("2026-01-01T00:00:00Z")
  private val finishedAt = Instant.parse("2026-01-01T00:00:01Z")

  test("completeJudging rejects terminal updates without judgeResult") {
    val running = runningState()

    val result = SubmissionJudgeRules.completeJudging(
      running,
      SubmissionJudgeCompletion(
        status = SubmissionStatus.Completed,
        judgeResult = None
      ),
      finishedAt
    )

    assertEquals(result.left.toOption, Some("Terminal judge updates must include judgeResult."))
  }

  test("completeJudging rejects completed status with system error result") {
    val running = runningState()

    val result = SubmissionJudgeRules.completeJudging(
      running,
      SubmissionJudgeCompletion(
        status = SubmissionStatus.Completed,
        judgeResult = Some(judgeResult(SubmissionVerdict.SystemError))
      ),
      finishedAt
    )

    assertEquals(result.left.toOption, Some("Completed judge updates must not include a system error judgeResult."))
  }

  test("completeJudging rejects failed status without system error result") {
    val running = runningState()

    val result = SubmissionJudgeRules.completeJudging(
      running,
      SubmissionJudgeCompletion(
        status = SubmissionStatus.Failed,
        judgeResult = Some(judgeResult(SubmissionVerdict.CompileError))
      ),
      finishedAt
    )

    assertEquals(result.left.toOption, Some("Failed judge updates must include a system error judgeResult."))
  }

  test("completeJudging accepts consistent completed result") {
    val running = runningState()
    val acceptedResult = judgeResult(SubmissionVerdict.Accepted)

    val result = SubmissionJudgeRules.completeJudging(
      running,
      SubmissionJudgeCompletion(
        status = SubmissionStatus.Completed,
        judgeResult = Some(acceptedResult)
      ),
      finishedAt
    )

    assertEquals(
      result.map(state => (state.status, state.judgeResult, state.finishedAt)),
      Right((SubmissionStatus.Completed, Some(acceptedResult), Some(finishedAt)))
    )
  }

  private def runningState(): SubmissionJudgeState =
    SubmissionJudgeRules.beginJudging(SubmissionJudgeState.queued, startedAt).toOption.get

  private def judgeResult(verdict: SubmissionVerdict): JudgeResult =
    JudgeResult(
      score = if verdict == SubmissionVerdict.Accepted then BigDecimal(1) else BigDecimal(0),
      verdict = verdict,
      message = None,
      timeUsedMs = None,
      memoryUsedKb = None,
      subtasks = Nil
    )
