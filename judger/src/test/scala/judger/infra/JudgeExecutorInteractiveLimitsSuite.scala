package judger.infra

import judgeprotocol.objects.{SubmissionVerdict, TestcaseMemoryLimitMb, TestcaseTimeLimitMs}
import judgeprotocol.objects.response.{JudgeTaskChecker, JudgeTaskFileRef, JudgeTaskLimits, JudgeTaskTestcase, JudgeTaskTool, JudgeTaskToolLimits}
import judger.objects.ProcessResult
import munit.FunSuite

class JudgeExecutorInteractiveLimitsSuite extends FunSuite:

  test("tool CPU timeout is protocol-side success") {
    val interactor = tool("tools/interactor.cpp", timeMs = 1000)
    val provider = tool("tools/strategy.cpp", timeMs = 500)

    assert(
      JudgeExecutor.interactiveToolCpuLimitExceeded(
        interactor,
        Some(provider),
        cpuTimeout(timeUsedMs = 1000),
        Some(okResult())
      )
    )
    assert(
      JudgeExecutor.interactiveToolCpuLimitExceeded(
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

    assertEquals(JudgeExecutor.interactiveToolCpuLimitExceeded(interactor, None, wallOnly, None), false)
    assertEquals(
      JudgeExecutor.interactiveWallOnlyVerdict(
        participants = Map.empty,
        participantCpuLimitMs = 1000,
        processes = List(wallOnly -> 1000L),
        fallback = wallOnly
      ).map(_._1),
      Some(SubmissionVerdict.IdlenessLimitExceeded)
    )
  }

  test("wall-only timeout reports participant runtime error first") {
    val wallOnly = wallTimeout(timeUsedMs = 25)
    val runtimeError = okResult(exitCode = Some(1))

    assertEquals(
      JudgeExecutor.interactiveWallOnlyVerdict(
        participants = Map("main" -> runtimeError),
        participantCpuLimitMs = 1000,
        processes = List(wallOnly -> 1000L, runtimeError -> 1000L),
        fallback = wallOnly
      ).map(_._1),
      Some(SubmissionVerdict.RuntimeError)
    )
  }

  test("participant CPU timeout remains time limit exceeded") {
    val participant = cpuTimeout(timeUsedMs = 1000)

    assertEquals(
      JudgeExecutor.participantFailure(Map("main" -> participant), timeLimitMs = 1000).map(_._1),
      Some(SubmissionVerdict.TimeLimitExceeded)
    )
  }

  test("shared interactive wall budget includes roles interactor and strategy provider") {
    val testcase = testcaseWithLimits(timeMs = 1000)
    val interactor = tool("tools/interactor.cpp", timeMs = 300)
    val provider = tool("tools/strategy.cpp", timeMs = 500)

    assertEquals(
      JudgeExecutor.interactiveWallTimeLimitMs(testcase, roleCount = 2, interactor, Some(provider)),
      4700L
    )
  }

  test("sandbox CPU time does not fall back to wall time") {
    assertEquals(IsolateSandbox.timeUsedMs(Map("time-wall" -> "4.2")), None)
    assertEquals(IsolateSandbox.wallTimeUsedMs(Map("time-wall" -> "4.2")), Some(4200L))
  }

  private def testcaseWithLimits(timeMs: Int): JudgeTaskTestcase =
    JudgeTaskTestcase(
      index = 1,
      label = None,
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
