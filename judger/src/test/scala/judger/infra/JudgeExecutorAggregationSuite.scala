package judger.infra

import judgeprotocol.objects.{ProblemSlug, SubmissionId, SubmissionVerdict, TestcaseMemoryLimitMb, TestcaseTimeLimitMs}
import judgeprotocol.objects.response.*
import munit.FunSuite

class JudgeExecutorAggregationSuite extends FunSuite:

  test("sum subtask keeps aggregate score and tracks testcase minimum as lowest score") {
    val targetSubtask = subtask(
      index = 1,
      scoreRatio = BigDecimal(1),
      testcases = (1 to 5).toList.map(index => testcase(index, JudgeTestcaseType.Main, BigDecimal("0.2")))
    )
    val baseTestcases =
      (1 to 4).toList.map(index => testcaseResult(index, JudgeTestcaseType.Main, BigDecimal(1), SubmissionVerdict.Accepted)) :+
        testcaseResult(5, JudgeTestcaseType.Main, BigDecimal(0), SubmissionVerdict.WrongAnswer)

    val baseResult = JudgeExecutor.aggregateSubtask(targetSubtask, baseTestcases)
    assertEquals(baseResult.score, BigDecimal("0.8"))
    assertEquals(baseResult.lowestScore, BigDecimal(0))
    assertEquals(baseResult.baseResult, None)

    val passingHackResult = JudgeExecutor.aggregateSubtask(
      targetSubtask,
      baseTestcases :+ testcaseResult(6, JudgeTestcaseType.Hack, BigDecimal(1), SubmissionVerdict.Accepted)
    )
    assertEquals(passingHackResult.score, BigDecimal("0.8"))
    assertEquals(passingHackResult.lowestScore, BigDecimal(0))
    assertEquals(passingHackResult.baseResult.map(_.score), Some(BigDecimal("0.8")))
    assertEquals(passingHackResult.baseResult.map(_.lowestScore), Some(BigDecimal(0)))

    val failingHackResult = JudgeExecutor.aggregateSubtask(
      targetSubtask,
      baseTestcases :+ testcaseResult(6, JudgeTestcaseType.Hack, BigDecimal(0), SubmissionVerdict.WrongAnswer)
    )
    assertEquals(failingHackResult.score, BigDecimal(0))
    assertEquals(failingHackResult.lowestScore, BigDecimal(0))
    assertEquals(failingHackResult.baseResult.map(_.score), Some(BigDecimal("0.8")))
    assertEquals(failingHackResult.baseResult.map(_.lowestScore), Some(BigDecimal(0)))
  }

  test("sum task aggregates subtask lowest scores with task score aggregation") {
    val firstSubtask = subtask(
      index = 1,
      scoreRatio = BigDecimal("0.5"),
      testcases = (1 to 5).toList.map(index => testcase(index, JudgeTestcaseType.Main, BigDecimal("0.2")))
    )
    val secondSubtask = subtask(
      index = 2,
      scoreRatio = BigDecimal("0.5"),
      testcases = List(testcase(1, JudgeTestcaseType.Main, BigDecimal(1)))
    )
    val targetTask = task(firstSubtask, secondSubtask)
    val firstBaseTestcases =
      (1 to 4).toList.map(index => testcaseResult(index, JudgeTestcaseType.Main, BigDecimal(1), SubmissionVerdict.Accepted)) :+
        testcaseResult(5, JudgeTestcaseType.Main, BigDecimal(0), SubmissionVerdict.WrongAnswer)
    val secondResult = JudgeExecutor.aggregateSubtask(
      secondSubtask,
      List(testcaseResult(1, JudgeTestcaseType.Main, BigDecimal(1), SubmissionVerdict.Accepted))
    )

    val firstPassingHack = JudgeExecutor.aggregateSubtask(
      firstSubtask,
      firstBaseTestcases :+ testcaseResult(6, JudgeTestcaseType.Hack, BigDecimal(1), SubmissionVerdict.Accepted)
    )
    val passingTask = JudgeExecutor.aggregateTask(targetTask, List(firstPassingHack, secondResult))
    assertEquals(passingTask.score, BigDecimal("0.9"))
    assertEquals(passingTask.lowestScore, BigDecimal("0.5"))
    assertEquals(passingTask.baseResult.map(_.score), Some(BigDecimal("0.9")))
    assertEquals(passingTask.baseResult.map(_.lowestScore), Some(BigDecimal("0.5")))

    val firstFailingHack = JudgeExecutor.aggregateSubtask(
      firstSubtask,
      firstBaseTestcases :+ testcaseResult(6, JudgeTestcaseType.Hack, BigDecimal(0), SubmissionVerdict.WrongAnswer)
    )
    val failingTask = JudgeExecutor.aggregateTask(targetTask, List(firstFailingHack, secondResult))
    assertEquals(failingTask.score, BigDecimal(0))
    assertEquals(failingTask.lowestScore, BigDecimal(0))
    assertEquals(failingTask.baseResult.map(_.score), Some(BigDecimal("0.9")))
    assertEquals(failingTask.baseResult.map(_.lowestScore), Some(BigDecimal("0.5")))
  }

  private def task(subtasks: JudgeTaskSubtask*): JudgeTask =
    JudgeTask(
      submissionId = SubmissionId(1),
      problemSlug = ProblemSlug("two-sum"),
      programs = Map.empty,
      problemDataVersion = "v1",
      roundingScale = 2,
      aggregation = JudgeTaskAggregation("sum", "max", "max"),
      subtasks = subtasks.toList
    )

  private def subtask(index: Int, scoreRatio: BigDecimal, testcases: List[JudgeTaskTestcase]): JudgeTaskSubtask =
    JudgeTaskSubtask(
      index = index,
      label = None,
      scoreRatio = scoreRatio,
      mode = JudgeTaskMode.traditional("main"),
      validator = None,
      standard = None,
      aggregation = JudgeTaskAggregation("sum", "max", "max"),
      testcases = testcases
    )

  private def testcase(index: Int, testcaseType: JudgeTestcaseType, scoreRatio: BigDecimal): JudgeTaskTestcase =
    JudgeTaskTestcase(
      index = index,
      label = None,
      testcaseType = testcaseType,
      scoreRatio = scoreRatio,
      limits = JudgeTaskLimits(TestcaseTimeLimitMs(1000), TestcaseMemoryLimitMb(256)),
      checker = JudgeTaskChecker("builtin", Some("exact"), None),
      input = JudgeTaskFileRef.unsafe(s"$index.in", 1L, "a" * 64),
      answer = Some(JudgeTaskFileRef.unsafe(s"$index.ans", 1L, "b" * 64)),
      strategyProvider = None
    )

  private def testcaseResult(
    index: Int,
    testcaseType: JudgeTestcaseType,
    score: BigDecimal,
    verdict: SubmissionVerdict
  ): JudgeTestcaseResult =
    JudgeTestcaseResult(
      index = index,
      label = None,
      testcaseType = testcaseType,
      score = score,
      verdict = verdict,
      message = None,
      reason = None,
      timeUsedMs = None,
      memoryUsedKb = None
    )
