package judger.infra

import judgeprotocol.objects.response.JudgeFailureReason
import munit.FunSuite

class CheckerRunnerSuite extends FunSuite:
  test("parseCheckerStdout accepts score with optional message") {
    val result = CheckerRunner.parseCheckerStdout("0.5 partial score\n")

    assertEquals(result.map(_.score), Right(BigDecimal("0.5")))
    assertEquals(result.map(_.message), Right(Some("partial score")))
  }

  test("parseCheckerStdout rejects invalid scores") {
    assertEquals(CheckerRunner.parseCheckerStdout("nan").left.toOption, Some(JudgeFailureReason.CheckerRuntimeFailed))
    assertEquals(CheckerRunner.parseCheckerStdout("1.5 too high").left.toOption, Some(JudgeFailureReason.CheckerRuntimeFailed))
  }

  test("normalizeOutput preserves line content while ignoring trailing newline style") {
    assertEquals(CheckerRunner.normalizeOutput("a\r\nb\r\n"), "a\nb")
    assertEquals(CheckerRunner.normalizeOutput("a\nb\n"), "a\nb")
  }
