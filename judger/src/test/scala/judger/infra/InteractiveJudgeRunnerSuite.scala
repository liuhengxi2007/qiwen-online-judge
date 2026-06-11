package judger.infra

import judgeprotocol.objects.{SubmissionVerdict, TestcaseMemoryLimitMb, TestcaseTimeLimitMs}
import judgeprotocol.objects.response.{JudgeTaskChecker, JudgeTaskFileRef, JudgeTaskLimits, JudgeTaskTestcase, JudgeTaskTool, JudgeTaskToolLimits, JudgeTestcaseType}
import judger.objects.{ProcessResult, RuntimeCommand}
import munit.FunSuite

import java.nio.file.Path

class InteractiveJudgeRunnerSuite extends FunSuite:

  test("tool CPU timeout is protocol-side success") {
    val interactor = tool("tools/interactor.cpp", timeMs = 1000)
    val provider = tool("tools/strategy.cpp", timeMs = 500)

    assert(
      InteractiveJudgeRunner.interactiveToolCpuLimitExceeded(
        interactor,
        Some(provider),
        cpuTimeout(timeUsedMs = 1000),
        Some(okResult())
      )
    )
    assert(
      InteractiveJudgeRunner.interactiveToolCpuLimitExceeded(
        interactor,
        Some(provider),
        okResult(),
        Some(cpuTimeout(timeUsedMs = 500))
      )
    )
  }

  test("wall-only timeout is not protocol-side success") {
    val interactor = tool("tools/interactor.cpp", timeMs = 1000)
    val wallOnly = wallTimeout(timeUsedMs = 25)

    assertEquals(InteractiveJudgeRunner.interactiveToolCpuLimitExceeded(interactor, None, wallOnly, None), false)
    assertEquals(
      InteractiveJudgeRunner.interactiveWallOnlyVerdict(
        participants = Nil,
        participantCpuLimitMs = 1000,
        processes = List(wallOnly -> 1000L),
        fallback = wallOnly
      ).map(_._1),
      Some(SubmissionVerdict.IdlenessLimitExceeded)
    )
  }

  test("strategy provider read wait at provider budget is protocol-side success") {
    val wallOnly = wallTimeout(timeUsedMs = 25)

    assertEquals(
      InteractiveJudgeRunner.interactiveWallOnlyVerdict(
        participants = Nil,
        participantCpuLimitMs = 1000,
        processes = List(wallOnly -> 1000L),
        fallback = wallOnly,
        strategyProviderReadWaitMs = Some(500L),
        strategyProviderIdleLimitMs = Some(500L)
      ).map(_._1),
      Some(SubmissionVerdict.AcceptedByProtocol)
    )
  }

  test("strategy provider read wait below provider budget remains idleness limit exceeded") {
    val wallOnly = wallTimeout(timeUsedMs = 25)

    assertEquals(
      InteractiveJudgeRunner.interactiveWallOnlyVerdict(
        participants = Nil,
        participantCpuLimitMs = 1000,
        processes = List(wallOnly -> 1000L),
        fallback = wallOnly,
        strategyProviderReadWaitMs = Some(499L),
        strategyProviderIdleLimitMs = Some(500L)
      ).map(_._1),
      Some(SubmissionVerdict.IdlenessLimitExceeded)
    )
  }

  test("wall-only timeout reports participant runtime error first") {
    val wallOnly = wallTimeout(timeUsedMs = 25)
    val runtimeError = okResult(exitCode = Some(1))

    assertEquals(
      InteractiveJudgeRunner.interactiveWallOnlyVerdict(
        participants = List("main" -> runtimeError),
        participantCpuLimitMs = 1000,
        processes = List(wallOnly -> 1000L, runtimeError -> 1000L),
        fallback = wallOnly,
        strategyProviderReadWaitMs = Some(1000L),
        strategyProviderIdleLimitMs = Some(500L)
      ).map(_._1),
      Some(SubmissionVerdict.RuntimeError)
    )
  }

  test("participant CPU timeout remains time limit exceeded") {
    val participant = cpuTimeout(timeUsedMs = 1000)

    assertEquals(
      InteractiveJudgeRunner.participantFailure(List("main" -> participant), timeLimitMs = 1000).map(_._1),
      Some(SubmissionVerdict.TimeLimitExceeded)
    )
  }

  test("duplicate interactive roles get distinct participant FIFOs") {
    val command = RuntimeCommand("/box/main", Nil, processLimit = 1)
    val participants = InteractiveJudgeRunner.interactiveParticipants(List("main", "main"), Map("main" -> command), Path.of("/work/interactive"))

    assertEquals(participants.map(_.role), List("main", "main"))
    assertEquals(participants.map(_.occurrenceIndex), List(1, 2))
    assertEquals(participants.map(_.toParticipant.getFileName.toString), List("to-participant-1-main", "to-participant-2-main"))
    assertEquals(participants.map(_.fromParticipant.getFileName.toString), List("from-participant-1-main", "from-participant-2-main"))
  }

  test("participant failure checks duplicate role instances") {
    val runtimeError = okResult(exitCode = Some(1))

    assertEquals(
      InteractiveJudgeRunner.participantFailure(List("main" -> okResult(), "main" -> runtimeError), timeLimitMs = 1000),
      Some(SubmissionVerdict.RuntimeError -> runtimeError)
    )
  }

  test("shared interactive wall budget includes roles interactor and strategy provider") {
    val testcase = testcaseWithLimits(timeMs = 1000)
    val interactor = tool("tools/interactor.cpp", timeMs = 300)
    val provider = tool("tools/strategy.cpp", timeMs = 500)

    assertEquals(
      InteractiveJudgeRunner.interactiveWallTimeLimitMs(testcase, roleCount = 2, interactor, Some(provider)),
      4700L
    )
  }

  test("sandbox CPU time does not fall back to wall time") {
    assertEquals(IsolateSandbox.timeUsedMs(Map("time-wall" -> "4.2")), None)
    assertEquals(IsolateSandbox.wallTimeUsedMs(Map("time-wall" -> "4.2")), Some(4200L))
  }

  test("strategy provider read monitor parser sums complete pairs") {
    val log =
      """begin 1 100
        |end 1 180 4
        |begin 2 250
        |end 2 300 0
        |""".stripMargin

    assertEquals(InteractiveJudgeRunner.strategyProviderReadWaitMs(log, interactorWallTimeUsedMs = Some(1000L)), 130L)
  }

  test("strategy provider read monitor parser closes pending begin with interactor wall time") {
    val log =
      """begin 1 100
        |end 1 150 4
        |begin 2 200
        |""".stripMargin

    assertEquals(InteractiveJudgeRunner.strategyProviderReadWaitMs(log, interactorWallTimeUsedMs = Some(500L)), 450L)
  }

  test("strategy provider read monitor parser ignores empty or uncaptured logs") {
    assertEquals(InteractiveJudgeRunner.strategyProviderReadWaitMs("", interactorWallTimeUsedMs = Some(1000L)), 0L)
    assertEquals(
      InteractiveJudgeRunner.strategyProviderReadWaitMs("begin 1 100\n", interactorWallTimeUsedMs = None),
      0L
    )
  }

  private def testcaseWithLimits(timeMs: Int): JudgeTaskTestcase =
    JudgeTaskTestcase(
      index = 1,
      label = None,
      testcaseType = JudgeTestcaseType.Main,
      scoreRatio = BigDecimal(1),
      limits = JudgeTaskLimits(TestcaseTimeLimitMs(timeMs), TestcaseMemoryLimitMb(256)),
      checker = JudgeTaskChecker("builtin", Some("exact"), None),
      input = fileRef("sample/1.in"),
      answer = Some(fileRef("sample/1.ans")),
      strategyProvider = None
    )

  private def tool(path: String, timeMs: Int): JudgeTaskTool =
    JudgeTaskTool(
      source = fileRef(path),
      limits = Some(JudgeTaskToolLimits(TestcaseTimeLimitMs(timeMs), TestcaseMemoryLimitMb(256)))
    )

  private def fileRef(path: String): JudgeTaskFileRef =
    JudgeTaskFileRef.unsafe(path, 1L, "a" * 64)

  private def okResult(exitCode: Option[Int] = Some(0)): ProcessResult =
    processResult(exitCode = exitCode, timedOut = false, timeUsedMs = Some(10L), wallTimeUsedMs = Some(10L))

  private def cpuTimeout(timeUsedMs: Long): ProcessResult =
    processResult(exitCode = None, timedOut = true, timeUsedMs = Some(timeUsedMs), wallTimeUsedMs = Some(timeUsedMs))

  private def wallTimeout(timeUsedMs: Long): ProcessResult =
    processResult(exitCode = None, timedOut = true, timeUsedMs = Some(timeUsedMs), wallTimeUsedMs = Some(10_000L))

  private def processResult(
    exitCode: Option[Int],
    timedOut: Boolean,
    timeUsedMs: Option[Long],
    wallTimeUsedMs: Option[Long]
  ): ProcessResult =
    ProcessResult(
      exitCode = exitCode,
      isolateStatus = None,
      isolateMessage = None,
      stdout = "",
      stderr = "",
      timedOut = timedOut,
      timeUsedMs = timeUsedMs,
      wallTimeUsedMs = wallTimeUsedMs,
      memoryUsedKb = None
    )
