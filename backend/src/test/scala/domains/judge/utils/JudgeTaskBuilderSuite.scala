package domains.judge.utils

import domains.problem.objects.response.ProblemDetail
import domains.problem.objects.{OtherUserSubmissionAccess, ProblemData, ProblemDataPath, ProblemId, ProblemSlug, ProblemStatementText, ProblemTitle}
import domains.problem.objects.internal.{ProblemDataManifest, ProblemDataManifestEntry}
import domains.user.objects.{DisplayName, UserIdentity, Username}
import domains.submission.objects.{SubmissionId, SubmissionLanguage, SubmissionSourceCode}
import domains.submission.objects.internal.{ClaimedSubmission, SubmissionProgramManifest}
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
      assertEquals(task.subtasks.head.aggregation.score, "sum")
      assertEquals(task.subtasks.head.aggregation.time, "max")
      assertEquals(task.subtasks.head.aggregation.memory, "max")
      assertEquals(task.subtasks.head.testcases.head.index, 1)
      assertEquals(task.subtasks.head.testcases.head.label, None)
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

    assertEquals(result.left.toOption, Some("Limits are required for subtask #1 testcase #1."))
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
        |      realTimeMs: 1000
        |      memoryMb: 256
        |strategyProvider:
        |  path: tools/strategy.cpp
        |  limits:
        |    realTimeMs: 2000
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
      assertEquals(interactor.limits.map(_.realTimeMs.value), Some(1000))
      assertEquals(provider.source.path.value, "tools/strategy.cpp")
      assertEquals(provider.limits.map(_.realTimeMs.value), Some(2000))
      assertEquals(provider.limits.map(_.memoryMb.value), Some(512))
    }
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
