package domains.submission.api

import domains.submission.objects.SubmissionStatus
import domains.submission.objects.internal.SubmissionJudgeState
import io.circe.parser.decode
import io.circe.syntax.*
import judgeprotocol.objects.SubmissionVerdict
import judgeprotocol.objects.response.{JudgeFailureReason, JudgeResult, JudgeResultSummary, JudgeSubtaskResult, JudgeTestcaseResult, JudgeTestcaseType}
import munit.FunSuite

import java.time.Instant

class SubmissionJudgeRulesSuite extends FunSuite:

  private val startedAt = Instant.parse("2026-01-01T00:00:00Z")
  private val finishedAt = Instant.parse("2026-01-01T00:00:01Z")

  test("completeJudging rejects terminal updates without judgeResult") {
    val running = runningState()

    val result = SubmissionJudgeRules.completeJudging(
      running,
      SubmissionStatus.Completed,
      None,
      finishedAt
    )

    assertEquals(result.left.toOption, Some("Terminal judge updates must include judgeResult."))
  }

  test("completeJudging rejects completed status with system error result") {
    val running = runningState()

    val result = SubmissionJudgeRules.completeJudging(
      running,
      SubmissionStatus.Completed,
      Some(judgeResult(SubmissionVerdict.SystemError, reason = Some(JudgeFailureReason.SystemError))),
      finishedAt
    )

    assertEquals(result.left.toOption, Some("Completed judge updates must not include a system error judgeResult."))
  }

  test("completeJudging rejects reason on non-system verdict") {
    val running = runningState()

    val result = SubmissionJudgeRules.completeJudging(
      running,
      SubmissionStatus.Completed,
      Some(judgeResult(SubmissionVerdict.Accepted, reason = Some(JudgeFailureReason.SystemError))),
      finishedAt
    )

    assertEquals(result.left.toOption, Some("judgeResult baseResult reason is only allowed with system_error verdict."))
  }

  test("completeJudging rejects system error without reason") {
    val running = runningState()

    val result = SubmissionJudgeRules.completeJudging(
      running,
      SubmissionStatus.Failed,
      Some(judgeResult(SubmissionVerdict.SystemError, reason = None)),
      finishedAt
    )

    assertEquals(result.left.toOption, Some("judgeResult baseResult system_error verdict must include reason."))
  }

  test("completeJudging rejects nested reason on non-system verdict") {
    val running = runningState()
    val resultWithNestedReason = judgeResult(
      SubmissionVerdict.Accepted,
      subtasks = List(
        JudgeSubtaskResult(
          index = 1,
          label = Some("sample"),
          baseResult = summary(SubmissionVerdict.Accepted),
          worstResult = summary(SubmissionVerdict.Accepted),
          testcases = List(
            JudgeTestcaseResult(
              index = 1,
              label = Some("1"),
              testcaseType = JudgeTestcaseType.Main,
              score = BigDecimal(1),
              verdict = SubmissionVerdict.Accepted,
              message = Some("checker report"),
              reason = Some(JudgeFailureReason.CheckerRuntimeFailed),
              timeUsedMs = None,
              memoryUsedKb = None
            )
          )
        )
      )
    )

    val result = SubmissionJudgeRules.completeJudging(
      running,
      SubmissionStatus.Completed,
      Some(resultWithNestedReason),
      finishedAt
    )

    assertEquals(result.left.toOption, Some("testcase 1 (1) reason is only allowed with system_error verdict."))
  }

  test("completeJudging rejects nested system error without reason") {
    val running = runningState()
    val resultWithNestedSystemError = judgeResult(
      SubmissionVerdict.SystemError,
      reason = Some(JudgeFailureReason.CheckerRuntimeFailed),
      subtasks = List(
        JudgeSubtaskResult(
          index = 1,
          label = Some("sample"),
          baseResult = summary(SubmissionVerdict.SystemError, reason = Some(JudgeFailureReason.CheckerRuntimeFailed)),
          worstResult = summary(SubmissionVerdict.SystemError, reason = None),
          testcases = Nil
        )
      )
    )

    val result = SubmissionJudgeRules.completeJudging(
      running,
      SubmissionStatus.Failed,
      Some(resultWithNestedSystemError),
      finishedAt
    )

    assertEquals(result.left.toOption, Some("subtask 1 (sample) worstResult system_error verdict must include reason."))
  }

  test("completeJudging rejects failed status without system error result") {
    val running = runningState()

    val result = SubmissionJudgeRules.completeJudging(
      running,
      SubmissionStatus.Failed,
      Some(judgeResult(SubmissionVerdict.CompileError)),
      finishedAt
    )

    assertEquals(result.left.toOption, Some("Failed judge updates must include a system error judgeResult."))
  }

  test("completeJudging accepts failed status with system error reason") {
    val running = runningState()
    val failedResult = judgeResult(SubmissionVerdict.SystemError, reason = Some(JudgeFailureReason.JudgerRuntimeFailed))

    val result = SubmissionJudgeRules.completeJudging(
      running,
      SubmissionStatus.Failed,
      Some(failedResult),
      finishedAt
    )

    assertEquals(
      result.map(state => (state.status, state.judgeResult, state.finishedAt)),
      Right((SubmissionStatus.Failed, Some(failedResult), Some(finishedAt)))
    )
  }

  test("completeJudging accepts consistent completed result") {
    val running = runningState()
    val acceptedResult = judgeResult(SubmissionVerdict.Accepted)

    val result = SubmissionJudgeRules.completeJudging(
      running,
      SubmissionStatus.Completed,
      Some(acceptedResult),
      finishedAt
    )

    assertEquals(
      result.map(state => (state.status, state.judgeResult, state.finishedAt)),
      Right((SubmissionStatus.Completed, Some(acceptedResult), Some(finishedAt)))
    )
  }

  test("JudgeResult encoder omits node-level verdict and reason") {
    val encoded = judgeResult(
      SubmissionVerdict.Accepted,
      subtasks = List(
        JudgeSubtaskResult(
          index = 1,
          label = Some("main"),
          baseResult = summary(SubmissionVerdict.Accepted),
          worstResult = summary(SubmissionVerdict.Accepted),
          testcases = Nil
        )
      )
    ).asJson

    assertEquals(encoded.hcursor.downField("verdict").focus, None)
    assertEquals(encoded.hcursor.downField("reason").focus, None)
    assertEquals(encoded.hcursor.downField("subtasks").downN(0).downField("verdict").focus, None)
    assertEquals(encoded.hcursor.downField("subtasks").downN(0).downField("reason").focus, None)
    assertEquals(encoded.hcursor.downField("baseResult").downField("verdict").as[SubmissionVerdict], Right(SubmissionVerdict.Accepted))
  }

  test("JudgeResult decoder backfills legacy node verdict and reason into summaries") {
    val raw =
      """{
        |  "baseResult": {
        |    "score": 0,
        |    "timeUsedMs": 12,
        |    "memoryUsedKb": 256
        |  },
        |  "worstResult": {
        |    "score": 0,
        |    "timeUsedMs": 12,
        |    "memoryUsedKb": 256
        |  },
        |  "verdict": "wrong_answer",
        |  "message": "Wrong answer.",
        |  "subtasks": [
        |    {
        |      "index": 1,
        |      "label": "sample",
        |      "baseResult": {
        |        "score": 0,
        |        "timeUsedMs": 12,
        |        "memoryUsedKb": 256
        |      },
        |      "worstResult": {
        |        "score": 0,
        |        "timeUsedMs": 12,
        |        "memoryUsedKb": 256
        |      },
        |      "verdict": "wrong_answer",
        |      "message": "Wrong answer on sample.",
        |      "testcases": [
        |        {
        |          "index": 1,
        |          "label": "1",
        |          "score": 0,
        |          "verdict": "wrong_answer",
        |          "message": "checker report",
        |          "timeUsedMs": 12,
        |          "memoryUsedKb": 256
        |        }
        |      ]
        |    }
        |  ]
        |}""".stripMargin

    val decoded = decode[JudgeResult](raw)

    val expectedSummary = JudgeResultSummary(BigDecimal(0), SubmissionVerdict.WrongAnswer, None, Some(12L), Some(256L))
    assertEquals(decoded.map(_.baseResult), Right(expectedSummary))
    assertEquals(decoded.map(_.worstResult), Right(expectedSummary))
    assertEquals(decoded.map(_.subtasks.head.baseResult), Right(expectedSummary))
    assertEquals(decoded.map(_.subtasks.head.worstResult), Right(expectedSummary))
    assertEquals(decoded.map(_.subtasks.head.testcases.head.reason), Right(None))
    assertEquals(decoded.map(_.subtasks.head.testcases.head.message), Right(Some("checker report")))
  }

  private def runningState(): SubmissionJudgeState =
    SubmissionJudgeRules.beginJudging(SubmissionJudgeState.queued, startedAt).toOption.get

  private def judgeResult(
    verdict: SubmissionVerdict,
    reason: Option[JudgeFailureReason] = None,
    subtasks: List[JudgeSubtaskResult] = Nil
  ): JudgeResult =
    val score = if verdict == SubmissionVerdict.Accepted then BigDecimal(1) else BigDecimal(0)
    JudgeResult(
      baseResult = summary(verdict, reason, score),
      worstResult = summary(verdict, reason, subtasks.map(_.worstResult.score).minOption.getOrElse(score)),
      subtasks = subtasks
    )

  private def summary(
    verdict: SubmissionVerdict,
    reason: Option[JudgeFailureReason] = None,
    score: BigDecimal = BigDecimal(1)
  ): JudgeResultSummary =
    JudgeResultSummary(score, verdict, reason, None, None)
