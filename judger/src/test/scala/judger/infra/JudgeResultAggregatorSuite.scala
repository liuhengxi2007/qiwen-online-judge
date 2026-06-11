package judger.infra

import judgeprotocol.objects.{ProblemSlug, SubmissionId, SubmissionVerdict, TestcaseMemoryLimitMb, TestcaseTimeLimitMs}
import judgeprotocol.objects.response.*
import munit.FunSuite

class JudgeResultAggregatorSuite extends FunSuite:

  test("subtask base result aggregates only main testcases and worst includes sample and hack") {
    val targetSubtask = subtask(
      index = 1,
      scoreRatio = BigDecimal(1),
      testcases = List(
        testcase(1, JudgeTestcaseType.Sample, BigDecimal(0)),
        testcase(2, JudgeTestcaseType.Main, BigDecimal("0.5")),
        testcase(3, JudgeTestcaseType.Main, BigDecimal("0.5")),
        testcase(4, JudgeTestcaseType.Hack, BigDecimal(0))
      )
    )
    val result = JudgeResultAggregator.aggregateSubtask(
      targetSubtask,
      List(
        testcaseResult(1, JudgeTestcaseType.Sample, BigDecimal(0), SubmissionVerdict.WrongAnswer, timeUsedMs = Some(100), memoryUsedKb = Some(500)),
        testcaseResult(2, JudgeTestcaseType.Main, BigDecimal(1), SubmissionVerdict.Accepted, timeUsedMs = Some(20), memoryUsedKb = Some(200)),
        testcaseResult(3, JudgeTestcaseType.Main, BigDecimal(1), SubmissionVerdict.Accepted, timeUsedMs = Some(30), memoryUsedKb = Some(300)),
        testcaseResult(4, JudgeTestcaseType.Hack, BigDecimal(0), SubmissionVerdict.WrongAnswer, timeUsedMs = Some(120), memoryUsedKb = Some(700))
      )
    )

    assertEquals(result.baseResult.score, BigDecimal(1))
    assertEquals(result.baseResult.verdict, SubmissionVerdict.Accepted)
    assertEquals(result.baseResult.timeUsedMs, Some(30L))
    assertEquals(result.baseResult.memoryUsedKb, Some(300L))
    assertEquals(result.worstResult.score, BigDecimal(0))
    assertEquals(result.worstResult.verdict, SubmissionVerdict.WrongAnswer)
    assertEquals(result.worstResult.timeUsedMs, Some(120L))
    assertEquals(result.worstResult.memoryUsedKb, Some(700L))
  }

  test("sum subtask worst time uses max testcase time times main testcase count") {
    val targetSubtask = subtask(
      index = 1,
      scoreRatio = BigDecimal(1),
      aggregation = JudgeTaskAggregation("sum", "sum", "max"),
      testcases = List(
        testcase(1, JudgeTestcaseType.Sample, BigDecimal(0)),
        testcase(2, JudgeTestcaseType.Main, BigDecimal("0.5")),
        testcase(3, JudgeTestcaseType.Main, BigDecimal("0.5"))
      )
    )

    val result = JudgeResultAggregator.aggregateSubtask(
      targetSubtask,
      List(
        testcaseResult(1, JudgeTestcaseType.Sample, BigDecimal(1), SubmissionVerdict.Accepted, timeUsedMs = Some(100)),
        testcaseResult(2, JudgeTestcaseType.Main, BigDecimal(1), SubmissionVerdict.Accepted, timeUsedMs = Some(20)),
        testcaseResult(3, JudgeTestcaseType.Main, BigDecimal(1), SubmissionVerdict.Accepted, timeUsedMs = Some(30))
      )
    )

    assertEquals(result.baseResult.timeUsedMs, Some(50L))
    assertEquals(result.worstResult.timeUsedMs, Some(200L))
  }

  test("sum task aggregates subtask worst scores with normal task aggregation") {
    val firstSubtask = subtask(
      index = 1,
      scoreRatio = BigDecimal("0.5"),
      testcases = List(
        testcase(1, JudgeTestcaseType.Main, BigDecimal(1)),
        testcase(2, JudgeTestcaseType.Hack, BigDecimal(0))
      )
    )
    val secondSubtask = subtask(
      index = 2,
      scoreRatio = BigDecimal("0.5"),
      testcases = List(testcase(1, JudgeTestcaseType.Main, BigDecimal(1)))
    )
    val targetTask = task(firstSubtask, secondSubtask)
    val firstResult = JudgeResultAggregator.aggregateSubtask(
      firstSubtask,
      List(
        testcaseResult(1, JudgeTestcaseType.Main, BigDecimal(1), SubmissionVerdict.Accepted),
        testcaseResult(2, JudgeTestcaseType.Hack, BigDecimal(0), SubmissionVerdict.WrongAnswer)
      )
    )
    val secondResult = JudgeResultAggregator.aggregateSubtask(
      secondSubtask,
      List(testcaseResult(1, JudgeTestcaseType.Main, BigDecimal(1), SubmissionVerdict.Accepted))
    )

    val taskResult = JudgeResultAggregator.aggregateTask(targetTask, List(firstResult, secondResult))

    assertEquals(taskResult.baseResult.score, BigDecimal(1))
    assertEquals(taskResult.baseResult.verdict, SubmissionVerdict.Accepted)
    assertEquals(taskResult.worstResult.score, BigDecimal("0.5"))
    assertEquals(taskResult.worstResult.verdict, SubmissionVerdict.WrongAnswer)
  }

  test("subtask and task summaries normalize accepted by protocol to accepted") {
    val targetSubtask = subtask(
      index = 1,
      scoreRatio = BigDecimal(1),
      testcases = List(testcase(1, JudgeTestcaseType.Main, BigDecimal(1)))
    )
    val subtaskResult = JudgeResultAggregator.aggregateSubtask(
      targetSubtask,
      List(testcaseResult(1, JudgeTestcaseType.Main, BigDecimal(1), SubmissionVerdict.AcceptedByProtocol))
    )
    val taskResult = JudgeResultAggregator.aggregateTask(task(targetSubtask), List(subtaskResult))

    assertEquals(subtaskResult.testcases.head.verdict, SubmissionVerdict.AcceptedByProtocol)
    assertEquals(subtaskResult.baseResult.verdict, SubmissionVerdict.Accepted)
    assertEquals(subtaskResult.worstResult.verdict, SubmissionVerdict.Accepted)
    assertEquals(taskResult.baseResult.verdict, SubmissionVerdict.Accepted)
    assertEquals(taskResult.worstResult.verdict, SubmissionVerdict.Accepted)
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

  private def subtask(
    index: Int,
    scoreRatio: BigDecimal,
    testcases: List[JudgeTaskTestcase],
    aggregation: JudgeTaskAggregation = JudgeTaskAggregation("sum", "max", "max")
  ): JudgeTaskSubtask =
    JudgeTaskSubtask(
      index = index,
      label = None,
      scoreRatio = scoreRatio,
      mode = JudgeTaskMode.traditional("main"),
      validator = None,
      standard = None,
      aggregation = aggregation,
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
    verdict: SubmissionVerdict,
    timeUsedMs: Option[Long] = None,
    memoryUsedKb: Option[Long] = None
  ): JudgeTestcaseResult =
    JudgeTestcaseResult(
      index = index,
      label = None,
      testcaseType = testcaseType,
      score = score,
      verdict = verdict,
      message = None,
      reason = None,
      timeUsedMs = timeUsedMs,
      memoryUsedKb = memoryUsedKb
    )
