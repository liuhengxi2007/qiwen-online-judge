package judger.objects

import munit.FunSuite

class SandboxRunSpecSuite extends FunSuite:
  test("run spec carries command limits and explicit IO mode") {
    val command = RuntimeCommand("/box/main", List("--case", "1"), processLimit = 2)
    val limits = SandboxLimits.runtime(timeLimitMs = 1000, memoryLimitMb = 256)
    val spec = SandboxRunSpec(
      phase = "run-1",
      command = command,
      stdin = SandboxStdin.File("input.txt"),
      stdout = SandboxStdout.File("output.txt", capture = false),
      limits = limits,
      boxOffset = 3
    )

    assertEquals(spec.command, command)
    assertEquals(spec.stdin, SandboxStdin.File("input.txt"))
    assertEquals(spec.stdout, SandboxStdout.File("output.txt", capture = false))
    assertEquals(spec.limits, limits)
    assertEquals(spec.boxOffset, 3)
  }
