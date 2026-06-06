package domains.judge.utils

import domains.problem.objects.response.ProblemDetail
import domains.problem.objects.{OtherUserSubmissionAccess, ProblemData, ProblemDataPath, ProblemId, ProblemSlug, ProblemStatementText, ProblemTitle}
import domains.problem.objects.internal.{ProblemDataManifest, ProblemDataManifestEntry}
import domains.user.objects.{DisplayName, UserIdentity, Username}
import domains.submission.objects.{SubmissionId, SubmissionLanguage, SubmissionResultDisplayMode, SubmissionSourceCode}
import domains.submission.objects.internal.{ClaimedSubmission, SubmissionProgramManifest}
import judgeprotocol.objects.response.JudgeTestcaseType
import munit.FunSuite
import shared.objects.access.{BaseAccess, ResourceAccessPolicy}

import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.UUID

class JudgeTaskBuilderSuite extends FunSuite:

  private val sourceCode = SubmissionSourceCode("int main() {}")

  private val claimedSubmission = ClaimedSubmission(
    id = SubmissionId(1),
    problemId = ProblemId(UUID.fromString("11111111-1111-4111-8111-111111111111")),
    problemSlug = ProblemSlug("sample-problem"),
    programManifest = SubmissionProgramManifest.singleDefault(
      UUID.fromString("22222222-2222-4222-8222-222222222222"),
      SubmissionLanguage.Cpp17,
      sourceCode
    )
  )

  private val textSourceCode = SubmissionSourceCode("42\n")

  private val claimedSubmissionWithText = claimedSubmission.copy(
    programManifest = SubmissionProgramManifest.unsafeFromPrograms(
      UUID.fromString("33333333-3333-4333-8333-333333333333"),
      Map(
        "chain.txt" -> (SubmissionLanguage.Text -> textSourceCode),
        "main" -> (SubmissionLanguage.Cpp17 -> sourceCode)
      )
    )
  )

  private val manifest = ProblemDataManifest.fromEntries(
    claimedSubmission.problemSlug,
    List(
      entry("validators/validator.cpp"),
      entry("tools/interactor.cpp"),
      entry("tools/strategy.cpp"),
      entry("sample/1.in"),
      entry("sample/1.ans")
    )
  )

  private val problem = ProblemDetail(
    id = claimedSubmission.problemId,
    slug = claimedSubmission.problemSlug,
    title = ProblemTitle("Sample Problem"),
    statement = ProblemStatementText("Solve it."),
    data = ProblemData(None),
    ready = false,
    accessPolicy = ResourceAccessPolicy(BaseAccess.Restricted, Nil, Nil),
    otherUserSubmissionAccess = OtherUserSubmissionAccess.None,
    author = Some(UserIdentity(Username("creator"), DisplayName("Creator"))),
    canManage = true,
    createdAt = Instant.EPOCH,
    updatedAt = Instant.EPOCH
  )

  test("parseConfigBytes accepts enum aggregation names") {
    val result = JudgeTaskBuilder.parseConfigBytes(
      yaml("""
        |version: 2
        |limits:
        |  timeMs: 1000
        |  memoryMb: 256
        |validator:
        |  path: validators/validator.cpp
        |checker:
        |  type: builtin
        |  name: exact
        |aggregation:
        |  testcases: sum_max_max
        |  subtasks: sum_max_max
        |subtasks:
        |  - label: sample
        |    testcases:
        |      - input: sample/1.in
        |        answer: sample/1.ans
        |"""),
      claimedSubmission,
      sourceCode,
      manifest
    )

    assert(result.isRight)
    result.foreach { task =>
      assertEquals(task.aggregation.score, "sum")
      assertEquals(task.aggregation.time, "max")
      assertEquals(task.aggregation.memory, "max")
      assertEquals(task.subtasks.head.index, 1)
      assertEquals(task.subtasks.head.label, Some("sample"))
      assertEquals(task.subtasks.head.validator.map(_.source.path.value), Some("validators/validator.cpp"))
      assertEquals(task.subtasks.head.aggregation.score, "sum")
      assertEquals(task.subtasks.head.aggregation.time, "max")
      assertEquals(task.subtasks.head.aggregation.memory, "max")
      assertEquals(task.subtasks.head.testcases.head.index, 1)
      assertEquals(task.subtasks.head.testcases.head.label, None)
    }
  }

  test("parseConfigBytes accepts missing validator") {
    val result = JudgeTaskBuilder.parseConfigBytes(
      yaml("""
        |version: 2
        |limits:
        |  timeMs: 1000
        |  memoryMb: 256
        |checker:
        |  type: builtin
        |  name: exact
        |aggregation:
        |  testcases: sum_max_max
        |subtasks:
        |  - testcases:
        |      - input: sample/1.in
        |        answer: sample/1.ans
        |"""),
      claimedSubmission,
      sourceCode,
      manifest
    )

    assert(result.isRight)
    result.foreach { task =>
      assertEquals(task.subtasks.head.validator, None)
    }
  }

  test("parseConfigBytes rejects old comma aggregation values") {
    val result = JudgeTaskBuilder.parseConfigBytes(
      yaml("""
        |version: 2
        |limits:
        |  timeMs: 1000
        |  memoryMb: 256
        |validator:
        |  path: validators/validator.cpp
        |checker:
        |  type: builtin
        |  name: exact
        |aggregation:
        |  testcases: sum,max,max
        |subtasks:
        |  - testcases:
        |      - input: sample/1.in
        |        answer: sample/1.ans
        |"""),
      claimedSubmission,
      sourceCode,
      manifest
    )

    assertEquals(
      result.left.toOption,
      Some("Unsupported aggregation: sum,max,max. Expected one of: min_max_max, min_sum_max, sum_max_max, sum_sum_max.")
    )
  }

  test("parseConfigBytes rejects missing inherited limits") {
    val result = JudgeTaskBuilder.parseConfigBytes(
      yaml("""
        |version: 2
        |validator:
        |  path: validators/validator.cpp
        |checker:
        |  type: builtin
        |  name: exact
        |subtasks:
        |  - label: sample
        |    testcases:
        |      - input: sample/1.in
        |        answer: sample/1.ans
        |"""),
      claimedSubmission,
      sourceCode,
      manifest
    )

    assertEquals(result.left.toOption, Some("Limits are required for subtask 1 (sample) testcase 1."))
  }

  test("parseConfigBytes rejects testcase-level validator") {
    val result = JudgeTaskBuilder.parseConfigBytes(
      yaml("""
        |version: 2
        |limits:
        |  timeMs: 1000
        |  memoryMb: 256
        |checker:
        |  type: builtin
        |  name: exact
        |subtasks:
        |  - testcases:
        |      - input: sample/1.in
        |        answer: sample/1.ans
        |        validator:
        |          path: validators/validator.cpp
        |"""),
      claimedSubmission,
      sourceCode,
      manifest
    )

    assertEquals(result.left.toOption, Some("validator cannot be declared on subtask 1 testcase 1."))
  }

  test("validateReadyConfigBytes returns judge.yaml and referenced file paths") {
    val result = JudgeTaskBuilder.validateReadyConfigBytes(
      yaml("""
        |version: 2
        |limits:
        |  timeMs: 1000
        |  memoryMb: 256
        |validator:
        |  path: validators/validator.cpp
        |checker:
        |  type: builtin
        |  name: exact
        |subtasks:
        |  - label: sample
        |    testcases:
        |      - input: sample/1.in
        |        answer: sample/1.ans
        |"""),
      problem,
      manifest
    )

    assertEquals(
      result.map(_.retainedPaths),
      Right(Set(ProblemDataPath("judge.yaml"), ProblemDataPath("validators/validator.cpp"), ProblemDataPath("sample/1.in"), ProblemDataPath("sample/1.ans")))
    )
  }

  test("validateReadyConfigBytes derives verdict display for a single min testcase-aggregated subtask") {
    val result = JudgeTaskBuilder.validateReadyConfigBytes(
      yaml("""
        |version: 2
        |limits:
        |  timeMs: 1000
        |  memoryMb: 256
        |checker:
        |  type: builtin
        |  name: exact
        |aggregation:
        |  testcases: min_max_max
        |subtasks:
        |  - testcases:
        |      - input: sample/1.in
        |        answer: sample/1.ans
        |"""),
      problem,
      manifest
    )

    assertEquals(result.map(_.resultDisplayMode), Right(SubmissionResultDisplayMode.Verdict))
  }

  test("validateReadyConfigBytes derives score display for multi-subtask and sum testcase aggregation") {
    val multiSubtask = JudgeTaskBuilder.validateReadyConfigBytes(
      yaml("""
        |version: 2
        |limits:
        |  timeMs: 1000
        |  memoryMb: 256
        |checker:
        |  type: builtin
        |  name: exact
        |aggregation:
        |  testcases: min_sum_max
        |subtasks:
        |  - testcases:
        |      - input: sample/1.in
        |        answer: sample/1.ans
        |  - testcases:
        |      - input: sample/1.in
        |        answer: sample/1.ans
        |"""),
      problem,
      manifest
    )
    val sumAggregation = JudgeTaskBuilder.validateReadyConfigBytes(
      yaml("""
        |version: 2
        |limits:
        |  timeMs: 1000
        |  memoryMb: 256
        |checker:
        |  type: builtin
        |  name: exact
        |aggregation:
        |  testcases: sum_max_max
        |subtasks:
        |  - testcases:
        |      - input: sample/1.in
        |        answer: sample/1.ans
        |"""),
      problem,
      manifest
    )

    assertEquals(multiSubtask.map(_.resultDisplayMode), Right(SubmissionResultDisplayMode.Score))
    assertEquals(sumAggregation.map(_.resultDisplayMode), Right(SubmissionResultDisplayMode.Score))
  }

  test("parseConfigBytes rejects scoreRatio on sample and hack testcases") {
    val sampleResult = JudgeTaskBuilder.parseConfigBytes(
      yaml("""
        |version: 2
        |limits:
        |  timeMs: 1000
        |  memoryMb: 256
        |checker:
        |  type: builtin
        |  name: exact
        |aggregation:
        |  testcases: sum_max_max
        |subtasks:
        |  - testcases:
        |      - type: sample
        |        scoreRatio: 0
        |        input: sample/1.in
        |        answer: sample/1.ans
        |      - input: sample/1.in
        |        answer: sample/1.ans
        |"""),
      claimedSubmission,
      sourceCode,
      manifest
    )
    val hackResult = JudgeTaskBuilder.parseConfigBytes(
      yaml("""
        |version: 2
        |limits:
        |  timeMs: 1000
        |  memoryMb: 256
        |checker:
        |  type: builtin
        |  name: exact
        |aggregation:
        |  testcases: sum_max_max
        |subtasks:
        |  - testcases:
        |      - type: hack
        |        scoreRatio: 0
        |        input: sample/1.in
        |        answer: sample/1.ans
        |      - input: sample/1.in
        |        answer: sample/1.ans
        |"""),
      claimedSubmission,
      sourceCode,
      manifest
    )

    assertEquals(sampleResult.left.toOption, Some("scoreRatio cannot be declared on subtask 1 testcase 1 when type is sample."))
    assertEquals(hackResult.left.toOption, Some("scoreRatio cannot be declared on subtask 1 testcase 1 when type is hack."))
  }

  test("parseConfigBytes rejects subtasks without a main testcase") {
    val result = JudgeTaskBuilder.parseConfigBytes(
      yaml("""
        |version: 2
        |limits:
        |  timeMs: 1000
        |  memoryMb: 256
        |checker:
        |  type: builtin
        |  name: exact
        |aggregation:
        |  testcases: sum_max_max
        |subtasks:
        |  - label: sample
        |    testcases:
        |      - type: sample
        |        input: sample/1.in
        |        answer: sample/1.ans
        |"""),
      claimedSubmission,
      sourceCode,
      manifest
    )

    assertEquals(result.left.toOption, Some("subtask 1 (sample) must define at least one main testcase."))
  }

  test("parseConfigBytes distributes missing testcase weights only across main testcases") {
    val result = JudgeTaskBuilder.parseConfigBytes(
      yaml("""
        |version: 2
        |limits:
        |  timeMs: 1000
        |  memoryMb: 256
        |checker:
        |  type: builtin
        |  name: exact
        |aggregation:
        |  testcases: sum_max_max
        |subtasks:
        |  - testcases:
        |      - type: sample
        |        input: sample/1.in
        |        answer: sample/1.ans
        |      - input: sample/1.in
        |        answer: sample/1.ans
        |      - input: sample/1.in
        |        answer: sample/1.ans
        |"""),
      claimedSubmission,
      sourceCode,
      manifest
    )

    assertEquals(result.map(_.subtasks.head.testcases.map(_.testcaseType)), Right(List(JudgeTestcaseType.Sample, JudgeTestcaseType.Main, JudgeTestcaseType.Main)))
    assertEquals(result.map(_.subtasks.head.testcases.map(_.scoreRatio)), Right(List(BigDecimal(0), BigDecimal("0.5"), BigDecimal("0.5"))))
  }

  test("parseConfigBytes accepts limited interactor and inherited strategy provider") {
    val result = JudgeTaskBuilder.parseConfigBytes(
      yaml("""
        |version: 2
        |mode:
        |  type: interactive
        |  roles: [main]
        |  interactor:
        |    path: tools/interactor.cpp
        |    limits:
        |      timeMs: 1000
        |      memoryMb: 256
        |strategyProvider:
        |  path: tools/strategy.cpp
        |  limits:
        |    timeMs: 2000
        |    memoryMb: 512
        |limits:
        |  timeMs: 1000
        |  memoryMb: 256
        |validator:
        |  path: validators/validator.cpp
        |checker:
        |  type: builtin
        |  name: exact
        |subtasks:
        |  - testcases:
        |      - input: sample/1.in
        |        answer: sample/1.ans
        |"""),
      claimedSubmission,
      sourceCode,
      manifest
    )

    assert(result.isRight)
    result.foreach { task =>
      val interactor = task.subtasks.head.mode.interactor.get
      val provider = task.subtasks.head.testcases.head.strategyProvider.get

      assertEquals(interactor.source.path.value, "tools/interactor.cpp")
      assertEquals(interactor.limits.map(_.timeMs.value), Some(1000))
      assertEquals(provider.source.path.value, "tools/strategy.cpp")
      assertEquals(provider.limits.map(_.timeMs.value), Some(2000))
      assertEquals(provider.limits.map(_.memoryMb.value), Some(512))
    }
  }

  test("parseConfigBytes accepts traditional testcase role fallback list with text role") {
    val result = JudgeTaskBuilder.parseConfigBytes(
      yaml("""
        |version: 2
        |mode:
        |  type: traditional
        |  role: main
        |limits:
        |  timeMs: 1000
        |  memoryMb: 256
        |checker:
        |  type: builtin
        |  name: exact
        |subtasks:
        |  - testcases:
        |      - roles: [chain.txt, main]
        |        input: sample/1.in
        |        answer: sample/1.ans
        |"""),
      claimedSubmissionWithText,
      Map("chain.txt" -> textSourceCode, "main" -> sourceCode),
      manifest
    )

    assertEquals(result.map(_.subtasks.head.testcases.head.roles), Right(List("chain.txt", "main")))
  }

  test("parseConfigBytes accepts traditional mode text role") {
    val result = JudgeTaskBuilder.parseConfigBytes(
      yaml("""
        |version: 2
        |mode:
        |  type: traditional
        |  role: chain.txt
        |limits:
        |  timeMs: 1000
        |  memoryMb: 256
        |checker:
        |  type: builtin
        |  name: exact
        |subtasks:
        |  - testcases:
        |      - input: sample/1.in
        |        answer: sample/1.ans
        |"""),
      claimedSubmissionWithText,
      Map("chain.txt" -> textSourceCode, "main" -> sourceCode),
      manifest
    )

    assertEquals(result.map(_.subtasks.head.mode.role), Right(Some("chain.txt")))
  }

  test("parseConfigBytes rejects text roles in interactive mode") {
    val result = JudgeTaskBuilder.parseConfigBytes(
      yaml("""
        |version: 2
        |mode:
        |  type: interactive
        |  roles: [chain.txt]
        |  interactor:
        |    path: tools/interactor.cpp
        |    limits:
        |      timeMs: 1000
        |      memoryMb: 256
        |limits:
        |  timeMs: 1000
        |  memoryMb: 256
        |checker:
        |  type: builtin
        |  name: exact
        |subtasks:
        |  - testcases:
        |      - input: sample/1.in
        |        answer: sample/1.ans
        |"""),
      claimedSubmissionWithText,
      Map("chain.txt" -> textSourceCode, "main" -> sourceCode),
      manifest
    )

    assertEquals(result.left.toOption, Some("Role must contain only ASCII letters, digits, '_' or '-': chain.txt."))
  }

  test("parseConfigBytes rejects invalid testcase text role names") {
    val result = JudgeTaskBuilder.parseConfigBytes(
      yaml("""
        |version: 2
        |limits:
        |  timeMs: 1000
        |  memoryMb: 256
        |checker:
        |  type: builtin
        |  name: exact
        |subtasks:
        |  - testcases:
        |      - roles: [a.b.txt]
        |        input: sample/1.in
        |        answer: sample/1.ans
        |"""),
      claimedSubmissionWithText,
      Map("chain.txt" -> textSourceCode, "main" -> sourceCode),
      manifest
    )

    assertEquals(
      result.left.toOption,
      Some("roles[0] Role must contain only ASCII letters, digits, '_' or '-', with an optional single '.txt' suffix: a.b.txt.")
    )
  }

  test("parseConfigBytes rejects testcase roles in interactive mode") {
    val result = JudgeTaskBuilder.parseConfigBytes(
      yaml("""
        |version: 2
        |mode:
        |  type: interactive
        |  roles: [main]
        |  interactor:
        |    path: tools/interactor.cpp
        |    limits:
        |      timeMs: 1000
        |      memoryMb: 256
        |limits:
        |  timeMs: 1000
        |  memoryMb: 256
        |checker:
        |  type: builtin
        |  name: exact
        |subtasks:
        |  - testcases:
        |      - roles: [main]
        |        input: sample/1.in
        |        answer: sample/1.ans
        |"""),
      claimedSubmission,
      sourceCode,
      manifest
    )

    assertEquals(result.left.toOption, Some("roles cannot be declared on subtask 1 testcase 1 when mode is interactive."))
  }

  test("parseConfigBytes rejects legacy real-time tool limits") {
    val result = JudgeTaskBuilder.parseConfigBytes(
      yaml("""
        |version: 2
        |mode:
        |  type: interactive
        |  roles: [main]
        |  interactor:
        |    path: tools/interactor.cpp
        |    limits:
        |      realTimeMs: 1000
        |      memoryMb: 256
        |limits:
        |  timeMs: 1000
        |  memoryMb: 256
        |validator:
        |  path: validators/validator.cpp
        |checker:
        |  type: builtin
        |  name: exact
        |subtasks:
        |  - testcases:
        |      - input: sample/1.in
        |        answer: sample/1.ans
        |"""),
      claimedSubmission,
      sourceCode,
      manifest
    )

    assertEquals(result.left.toOption, Some("mode.interactor.limits.timeMs is required."))
  }

  test("parseConfigBytes rejects interactive interactor without limits") {
    val result = JudgeTaskBuilder.parseConfigBytes(
      yaml("""
        |version: 2
        |mode:
        |  type: interactive
        |  roles: [main]
        |  interactor:
        |    path: tools/interactor.cpp
        |limits:
        |  timeMs: 1000
        |  memoryMb: 256
        |validator:
        |  path: validators/validator.cpp
        |checker:
        |  type: builtin
        |  name: exact
        |subtasks:
        |  - testcases:
        |      - input: sample/1.in
        |        answer: sample/1.ans
        |"""),
      claimedSubmission,
      sourceCode,
      manifest
    )

    assertEquals(result.left.toOption, Some("mode.interactor.limits is required."))
  }

  test("parseConfigBytes rejects strategy provider without limits") {
    val result = JudgeTaskBuilder.parseConfigBytes(
      yaml("""
        |version: 2
        |strategyProvider: tools/strategy.cpp
        |limits:
        |  timeMs: 1000
        |  memoryMb: 256
        |validator:
        |  path: validators/validator.cpp
        |checker:
        |  type: builtin
        |  name: exact
        |subtasks:
        |  - testcases:
        |      - input: sample/1.in
        |        answer: sample/1.ans
        |"""),
      claimedSubmission,
      sourceCode,
      manifest
    )

    assertEquals(result.left.toOption, Some("strategyProvider must be an object with path and limits."))
  }

  private def entry(path: String): ProblemDataManifestEntry =
    ProblemDataManifestEntry(ProblemDataPath(path), sizeBytes = 1L, sha256 = "a" * 64)

  private def yaml(content: String): Array[Byte] =
    content.stripMargin.trim.getBytes(StandardCharsets.UTF_8)
