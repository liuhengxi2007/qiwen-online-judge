package domains.judge.utils

import domains.problem.objects.response.ProblemDetail
import domains.problem.objects.{OtherUserSubmissionAccess, ProblemData, ProblemDataPath, ProblemId, ProblemSlug, ProblemStatementText, ProblemTitle}
import domains.problem.objects.internal.{ProblemDataManifest, ProblemDataManifestEntry}
import domains.user.objects.{DisplayName, UserIdentity, Username}
import domains.submission.objects.{SubmissionId, SubmissionLanguage, SubmissionResultDisplayMode, SubmissionSourceCode}
import domains.submission.objects.internal.{ClaimedSubmission, SubmissionProgramManifest}
import io.circe.Decoder
import io.circe.generic.semiauto.deriveDecoder
import io.circe.parser.decode
import judgeprotocol.objects.response.{JudgeTaskHackConfig, JudgeTestcaseType}
import munit.FunSuite
import shared.objects.access.{BaseAccess, ResourceAccessPolicy}

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Paths}
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
    programManifest = programManifest(
      UUID.fromString("33333333-3333-4333-8333-333333333333"),
      Map(
        "chain.txt" -> (SubmissionLanguage.Text -> textSourceCode),
        "main" -> (SubmissionLanguage.Cpp17 -> sourceCode)
      )
    )
  )

  private def programManifest(
    submissionUuid: UUID,
    rawPrograms: Map[String, (SubmissionLanguage, SubmissionSourceCode)]
  ): SubmissionProgramManifest =
    SubmissionProgramManifest.fromPrograms(submissionUuid, rawPrograms).fold(message => fail(message), identity)

  private val claimedSubmissionPython = claimedSubmission.copy(
    programManifest = SubmissionProgramManifest.singleDefault(
      UUID.fromString("44444444-4444-4444-8444-444444444444"),
      SubmissionLanguage.Python3,
      SubmissionSourceCode("print('ok')")
    )
  )

  private val manifest = ProblemDataManifest.fromEntries(
    claimedSubmission.problemSlug,
    List(
      entry("validators/validator.cpp"),
      entry("solutions/std.cpp"),
      entry("tools/interactor.cpp"),
      entry("tools/strategy.cpp"),
      entry("stubs/main.cpp"),
      entry("headers/xxx.h"),
      entry("headers/other/xxx.h"),
      entry("headers/readme.txt"),
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

  test("shared judge.yaml validation fixtures stay aligned") {
    val fixture = loadSharedValidationFixture()
    val fixtureManifest = ProblemDataManifest.fromEntries(claimedSubmission.problemSlug, fixture.files.map(entry))

    fixture.cases.foreach { testCase =>
      val result = JudgeTaskBuilder.parseConfigBytes(
        testCase.yaml.getBytes(StandardCharsets.UTF_8),
        claimedSubmission,
        sourceCode,
        fixtureManifest
      )

      assertEquals(result.isRight, testCase.valid, testCase.name)
    }
  }

  test("parseConfigBytes accepts enum aggregation names") {
    val result = JudgeTaskBuilder.parseConfigBytes(
      yaml("""
        |version: 2
        |hack: false
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
        |hack: false
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

  test("parseConfigBytes defaults hack to enabled and accepts standard false") {
    val result = JudgeTaskBuilder.parseConfigBytes(
      yaml("""
        |version: 2
        |validator:
        |  path: validators/validator.cpp
        |standard: false
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
      assertEquals(task.subtasks.head.hack, JudgeTaskHackConfig.WithoutAnswerGenerator)
      assertEquals(task.subtasks.head.standard, None)
    }
  }

  test("parseConfigBytes rejects enabled hack without validator") {
    val result = JudgeTaskBuilder.parseConfigBytes(
      yaml("""
        |version: 2
        |standard: false
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

    assertEquals(result.left.toOption, Some("Validator is required for subtask 1 when hack is enabled."))
  }

  test("parseConfigBytes rejects enabled hack with omitted standard") {
    val result = JudgeTaskBuilder.parseConfigBytes(
      yaml("""
        |version: 2
        |validator:
        |  path: validators/validator.cpp
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

    assertEquals(result.left.toOption, Some("standard must be declared as an answer generator object or false for subtask 1 when hack is enabled."))
  }

  test("parseConfigBytes accepts enabled hack with answer generator") {
    val result = JudgeTaskBuilder.parseConfigBytes(
      yaml("""
        |version: 2
        |hack: true
        |validator:
        |  path: validators/validator.cpp
        |standard:
        |  language: cpp17
        |  path: solutions/std.cpp
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
      assertEquals(task.subtasks.head.hack, JudgeTaskHackConfig.WithAnswerGenerator)
      assertEquals(task.subtasks.head.standard.map(_.source.path.value), Some("solutions/std.cpp"))
    }
  }

  test("parseConfigBytes allows subtask hack and standard overrides") {
    val result = JudgeTaskBuilder.parseConfigBytes(
      yaml("""
        |version: 2
        |hack: false
        |validator:
        |  path: validators/validator.cpp
        |standard: false
        |limits:
        |  timeMs: 1000
        |  memoryMb: 256
        |checker:
        |  type: builtin
        |  name: exact
        |aggregation:
        |  testcases: sum_max_max
        |subtasks:
        |  - label: disabled
        |    testcases:
        |      - input: sample/1.in
        |        answer: sample/1.ans
        |  - label: enabled
        |    hack: true
        |    standard:
        |      language: cpp17
        |      path: solutions/std.cpp
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
      assertEquals(task.subtasks.map(_.hack), List(JudgeTaskHackConfig.Disabled, JudgeTaskHackConfig.WithAnswerGenerator))
      assertEquals(task.subtasks(1).standard.map(_.source.path.value), Some("solutions/std.cpp"))
    }
  }

  test("parseConfigBytes rejects testcase-level hack declarations") {
    val result = JudgeTaskBuilder.parseConfigBytes(
      yaml("""
        |version: 2
        |hack: false
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
        |        hack: true
        |"""),
      claimedSubmission,
      sourceCode,
      manifest
    )

    assertEquals(result.left.toOption, Some("hack cannot be declared on subtask 1 testcase 1."))
  }

  test("parseConfigBytes rejects old comma aggregation values") {
    val result = JudgeTaskBuilder.parseConfigBytes(
      yaml("""
        |version: 2
        |hack: false
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
        |hack: false
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
        |hack: false
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
        |hack: false
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

  test("validateReadyConfigBytes retains declared role stub files") {
    val result = JudgeTaskBuilder.validateReadyConfigBytes(
      yaml("""
        |version: 2
        |hack: false
        |roles:
        |  helper:
        |    stubs:
        |      cpp17: stubs/main.cpp
        |limits:
        |  timeMs: 1000
        |  memoryMb: 256
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

    assertEquals(result.map(_.retainedPaths.contains(ProblemDataPath("stubs/main.cpp"))), Right(true))
  }

  test("validateReadyConfigBytes retains declared header files") {
    val result = JudgeTaskBuilder.validateReadyConfigBytes(
      yaml("""
        |version: 2
        |hack: false
        |headers:
        |  - headers/xxx.h
        |limits:
        |  timeMs: 1000
        |  memoryMb: 256
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

    assertEquals(result.map(_.retainedPaths.contains(ProblemDataPath("headers/xxx.h"))), Right(true))
  }

  test("validateReadyConfigBytes derives verdict display for a single min testcase-aggregated subtask") {
    val result = JudgeTaskBuilder.validateReadyConfigBytes(
      yaml("""
        |version: 2
        |hack: false
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
        |hack: false
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
        |hack: false
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
        |hack: false
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
        |hack: false
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

  test("parseConfigBytes accepts answerless hack testcase with builtin exact checker") {
    val manifestWithHack = ProblemDataManifest.fromEntries(
      claimedSubmission.problemSlug,
      manifest.entries :+ entry("hacks/7.in")
    )
    val result = JudgeTaskBuilder.parseConfigBytes(
      yaml("""
        |version: 2
        |hack: false
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
        |        input: hacks/7.in
        |      - input: sample/1.in
        |        answer: sample/1.ans
        |"""),
      claimedSubmission,
      sourceCode,
      manifestWithHack
    )

    assert(result.isRight)
    result.foreach { task =>
      val hackTestcase = task.subtasks.head.testcases.head
      assertEquals(hackTestcase.testcaseType, JudgeTestcaseType.Hack)
      assertEquals(hackTestcase.answer, None)
      assertEquals(hackTestcase.scoreRatio, BigDecimal(0))
    }
  }

  test("parseConfigBytes rejects subtasks without a main testcase") {
    val result = JudgeTaskBuilder.parseConfigBytes(
      yaml("""
        |version: 2
        |hack: false
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
        |hack: false
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
        |hack: false
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

  test("parseConfigBytes preserves duplicate interactive roles") {
    val result = JudgeTaskBuilder.parseConfigBytes(
      yaml("""
        |version: 2
        |hack: false
        |mode:
        |  type: interactive
        |  roles: [main, main]
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
      claimedSubmission,
      sourceCode,
      manifest
    )

    assertEquals(result.map(_.subtasks.head.mode.roles), Right(List("main", "main")))
  }

  test("parseConfigBytes accepts traditional testcase role fallback list with text role") {
    val result = JudgeTaskBuilder.parseConfigBytes(
      yaml("""
        |version: 2
        |hack: false
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

  test("parseConfigBytes attaches matching cpp17 role stub") {
    val result = JudgeTaskBuilder.parseConfigBytes(
      yaml("""
        |version: 2
        |hack: false
        |roles:
        |  main:
        |    stubs:
        |      cpp17: stubs/main.cpp
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
      claimedSubmission,
      sourceCode,
      manifest
    )

    assertEquals(result.map(_.programs("main").stub.map(_.path.value)), Right(Some("stubs/main.cpp")))
  }

  test("parseConfigBytes attaches declared headers to cpp17 programs") {
    val result = JudgeTaskBuilder.parseConfigBytes(
      yaml("""
        |version: 2
        |hack: false
        |headers:
        |  - headers/xxx.h
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
      claimedSubmission,
      sourceCode,
      manifest
    )

    assertEquals(result.map(_.programs("main").headers.map(_.path.value)), Right(List("headers/xxx.h")))
  }

  test("parseConfigBytes rejects invalid header declarations") {
    val missingHeader = JudgeTaskBuilder.parseConfigBytes(
      yaml("""
        |version: 2
        |hack: false
        |headers:
        |  - headers/missing.h
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
      claimedSubmission,
      sourceCode,
      manifest
    )
    val nonHeader = JudgeTaskBuilder.parseConfigBytes(
      yaml("""
        |version: 2
        |hack: false
        |headers:
        |  - headers/readme.txt
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
      claimedSubmission,
      sourceCode,
      manifest
    )
    val duplicateBasename = JudgeTaskBuilder.parseConfigBytes(
      yaml("""
        |version: 2
        |hack: false
        |headers:
        |  - headers/xxx.h
        |  - headers/other/xxx.h
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
      claimedSubmission,
      sourceCode,
      manifest
    )

    assertEquals(missingHeader.left.toOption, Some("Header file headers[0] does not exist: headers/missing.h."))
    assertEquals(nonHeader.left.toOption, Some("Header file headers[0] must end with .h: headers/readme.txt."))
    assertEquals(duplicateBasename.left.toOption, Some("headers must not declare duplicate include names: xxx.h."))
  }

  test("parseConfigBytes rejects invalid role stub declarations") {
    val missingStub = JudgeTaskBuilder.parseConfigBytes(
      yaml("""
        |version: 2
        |hack: false
        |roles:
        |  main:
        |    stubs:
        |      cpp17: stubs/missing.cpp
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
      claimedSubmission,
      sourceCode,
      manifest
    )
    val unsupportedLanguage = JudgeTaskBuilder.parseConfigBytes(
      yaml("""
        |version: 2
        |hack: false
        |roles:
        |  main:
        |    stubs:
        |      python3: stubs/main.py
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
      claimedSubmission,
      sourceCode,
      manifest
    )
    val textRole = JudgeTaskBuilder.parseConfigBytes(
      yaml("""
        |version: 2
        |hack: false
        |roles:
        |  chain.txt:
        |    stubs:
        |      cpp17: stubs/main.cpp
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
      claimedSubmission,
      sourceCode,
      manifest
    )

    assertEquals(missingStub.left.toOption, Some("Stub source file for role main language cpp17 does not exist: stubs/missing.cpp."))
    assertEquals(unsupportedLanguage.left.toOption, Some("roles.main.stubs.python3 is not supported. Only cpp17 stubs are supported."))
    assertEquals(textRole.left.toOption, Some("roles.chain.txt Role must contain only ASCII letters, digits, '_' or '-': chain.txt."))
  }

  test("parseConfigBytes rejects submitted languages without a matching role stub") {
    val result = JudgeTaskBuilder.parseConfigBytes(
      yaml("""
        |version: 2
        |hack: false
        |roles:
        |  main:
        |    stubs:
        |      cpp17: stubs/main.cpp
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
      claimedSubmissionPython,
      SubmissionSourceCode("print('ok')"),
      manifest
    )

    assertEquals(
      result.left.toOption,
      Some("Submission role main declares stubs but does not support language python3. Supported stub languages: cpp17.")
    )
  }

  test("parseConfigBytes accepts traditional mode text role") {
    val result = JudgeTaskBuilder.parseConfigBytes(
      yaml("""
        |version: 2
        |hack: false
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
        |hack: false
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
        |hack: false
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
        |hack: false
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
        |hack: false
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
        |hack: false
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
        |hack: false
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

  private def loadSharedValidationFixture(): SharedJudgeConfigValidationFixture =
    val path = Paths.get("..", "frontend", "src", "test", "fixtures", "judge-config-validation-cases.json")
    val raw = Files.readString(path, StandardCharsets.UTF_8)
    decode[SharedJudgeConfigValidationFixture](raw).fold(error => throw error, identity)

  private def yaml(content: String): Array[Byte] =
    content.stripMargin.trim.getBytes(StandardCharsets.UTF_8)

private final case class SharedJudgeConfigValidationFixture(
  files: List[String],
  cases: List[SharedJudgeConfigValidationCase]
)

private object SharedJudgeConfigValidationFixture:
  given Decoder[SharedJudgeConfigValidationFixture] = deriveDecoder

private final case class SharedJudgeConfigValidationCase(
  name: String,
  valid: Boolean,
  yaml: String
)

private object SharedJudgeConfigValidationCase:
  given Decoder[SharedJudgeConfigValidationCase] = deriveDecoder
