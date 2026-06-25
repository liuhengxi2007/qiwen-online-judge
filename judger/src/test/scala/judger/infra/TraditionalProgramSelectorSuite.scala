package judger.infra

import judgeprotocol.objects.{ProblemSlug, SubmissionId, SubmissionLanguage, SubmissionSourceCode, TestcaseMemoryLimitMb, TestcaseTimeLimitMs}
import judgeprotocol.objects.response.*
import judger.objects.RuntimeCommand
import munit.FunSuite

class TraditionalProgramSelectorSuite extends FunSuite:

  private val mainCommand = RuntimeCommand("/box/main", Nil, processLimit = 1)
  private val backupCommand = RuntimeCommand("/box/backup", Nil, processLimit = 1)

  test("selects text role before code role") {
    val selection = TraditionalProgramSelector.select(
      task = task(
        "chain.txt" -> JudgeTaskProgram(SubmissionLanguage.Text, SubmissionSourceCode("42\n")),
        "main" -> JudgeTaskProgram(SubmissionLanguage.Cpp17, SubmissionSourceCode("int main() {}"))
      ),
      subtask = subtask(),
      testcase = testcase(roles = List("chain.txt", "main")),
      programs = JudgeToolPreparation.PreparedPrograms(
        commands = Map("main" -> mainCommand),
        compileFailedRoles = Set.empty,
        textOutputs = Map("chain.txt" -> "42\n")
      )
    )

    assertEquals(selection, TraditionalProgramSelector.TraditionalProgramSelection.TextOutput("42\n"))
  }

  test("falls back when earlier text role is missing") {
    val selection = TraditionalProgramSelector.select(
      task = task("main" -> JudgeTaskProgram(SubmissionLanguage.Cpp17, SubmissionSourceCode("int main() {}"))),
      subtask = subtask(),
      testcase = testcase(roles = List("chain.txt", "main")),
      programs = JudgeToolPreparation.PreparedPrograms(
        commands = Map("main" -> mainCommand),
        compileFailedRoles = Set.empty,
        textOutputs = Map.empty
      )
    )

    assertEquals(selection, TraditionalProgramSelector.TraditionalProgramSelection.Command(mainCommand))
  }

  test("does not fall back after selected code role compile failure") {
    val selection = TraditionalProgramSelector.select(
      task = task(
        "main" -> JudgeTaskProgram(SubmissionLanguage.Cpp17, SubmissionSourceCode("int main() {}")),
        "chain.txt" -> JudgeTaskProgram(SubmissionLanguage.Text, SubmissionSourceCode("42\n"))
      ),
      subtask = subtask(),
      testcase = testcase(roles = List("main", "chain.txt")),
      programs = JudgeToolPreparation.PreparedPrograms(
        commands = Map.empty,
        compileFailedRoles = Set("main"),
        textOutputs = Map("chain.txt" -> "42\n")
      )
    )

    assertEquals(selection, TraditionalProgramSelector.TraditionalProgramSelection.CompileError)
  }

  test("uses mode role when testcase roles are absent") {
    val selection = TraditionalProgramSelector.select(
      task = task(
        "main" -> JudgeTaskProgram(SubmissionLanguage.Cpp17, SubmissionSourceCode("int main() {}")),
        "backup" -> JudgeTaskProgram(SubmissionLanguage.Cpp17, SubmissionSourceCode("int main() {}"))
      ),
      subtask = subtask(modeRole = "backup"),
      testcase = testcase(),
      programs = JudgeToolPreparation.PreparedPrograms(
        commands = Map("main" -> mainCommand, "backup" -> backupCommand),
        compileFailedRoles = Set.empty,
        textOutputs = Map.empty
      )
    )

    assertEquals(selection, TraditionalProgramSelector.TraditionalProgramSelection.Command(backupCommand))
  }

  test("returns compile error when all roles are missing") {
    val selection = TraditionalProgramSelector.select(
      task = task("main" -> JudgeTaskProgram(SubmissionLanguage.Cpp17, SubmissionSourceCode("int main() {}"))),
      subtask = subtask(),
      testcase = testcase(roles = List("chain.txt", "backup")),
      programs = JudgeToolPreparation.PreparedPrograms(
        commands = Map("main" -> mainCommand),
        compileFailedRoles = Set.empty,
        textOutputs = Map.empty
      )
    )

    assertEquals(selection, TraditionalProgramSelector.TraditionalProgramSelection.CompileError)
  }

  private def task(programs: (String, JudgeTaskProgram)*): JudgeTask =
    JudgeTask(
      submissionId = SubmissionId(1),
      problemSlug = ProblemSlug("sample"),
      startedAtEpochMilli = 0L,
      programs = programs.toMap,
      problemDataVersion = "v1",
      roundingScale = 6,
      aggregation = JudgeTaskAggregation("sum", "max", "max"),
      subtasks = Nil
    )

  private def subtask(modeRole: String = "main"): JudgeTaskSubtask =
    JudgeTaskSubtask(
      index = 1,
      label = None,
      scoreRatio = BigDecimal(1),
      mode = JudgeTaskMode.traditional(modeRole),
      validator = None,
      standard = None,
      aggregation = JudgeTaskAggregation("sum", "max", "max"),
      testcases = Nil
    )

  private def testcase(roles: List[String] = Nil): JudgeTaskTestcase =
    JudgeTaskTestcase(
      index = 1,
      label = None,
      testcaseType = JudgeTestcaseType.Main,
      scoreRatio = BigDecimal(1),
      limits = JudgeTaskLimits(TestcaseTimeLimitMs(1000), TestcaseMemoryLimitMb(256)),
      checker = JudgeTaskChecker("builtin", Some("exact"), None),
      input = fileRef("tests/1.in", "a" * 64),
      answer = Some(fileRef("tests/1.ans", "b" * 64)),
      strategyProvider = None,
      roles = roles
    )

  private def fileRef(path: String, sha256: String): JudgeTaskFileRef =
    JudgeTaskFileRef.from(path, 1L, sha256).fold(message => fail(message), identity)
