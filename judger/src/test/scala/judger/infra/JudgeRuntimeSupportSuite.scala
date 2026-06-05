package judger.infra

import judgeprotocol.objects.{ProblemSlug, SubmissionId, SubmissionLanguage, SubmissionSourceCode, SubmissionVerdict}
import judgeprotocol.objects.response.{JudgeFailureReason, JudgeTask, JudgeTaskAggregation, JudgeTaskMode, JudgeTaskProgram, JudgeTaskSubtask}
import munit.FunSuite

class JudgeRuntimeSupportSuite extends FunSuite:

  test("taskCompleted emits non-system verdict without reason") {
    val result = JudgeRuntimeSupport.taskCompleted(task, SubmissionVerdict.CompileError)
    val judgeResult = result.judgeResult.get

    assertEquals(result.status, judgeprotocol.objects.SubmissionStatus.Completed)
    assertEquals(judgeResult.verdict, SubmissionVerdict.CompileError)
    assertEquals(judgeResult.reason, None)
    assertEquals(judgeResult.subtasks.map(_.reason), List(None))
  }

  test("taskSystemError emits system_error with reason") {
    val result = JudgeRuntimeSupport.taskSystemError(task, JudgeFailureReason.JudgerRuntimeFailed)
    val judgeResult = result.judgeResult.get

    assertEquals(result.status, judgeprotocol.objects.SubmissionStatus.Failed)
    assertEquals(judgeResult.verdict, SubmissionVerdict.SystemError)
    assertEquals(judgeResult.reason, Some(JudgeFailureReason.JudgerRuntimeFailed))
    assertEquals(judgeResult.subtasks.map(_.reason), List(Some(JudgeFailureReason.JudgerRuntimeFailed)))
  }

  private val task: JudgeTask =
    JudgeTask(
      submissionId = SubmissionId(1),
      problemSlug = ProblemSlug("two-sum"),
      programs = Map("main" -> JudgeTaskProgram(SubmissionLanguage.Cpp17, SubmissionSourceCode("int main() { return 0; }"))),
      problemDataVersion = "v1",
      roundingScale = 2,
      aggregation = JudgeTaskAggregation("sum", "max", "max"),
      subtasks = List(
        JudgeTaskSubtask(
          index = 1,
          label = Some("sample"),
          scoreRatio = BigDecimal(1),
          mode = JudgeTaskMode.traditional("main"),
          aggregation = JudgeTaskAggregation("sum", "max", "max"),
          testcases = Nil
        )
      )
    )
