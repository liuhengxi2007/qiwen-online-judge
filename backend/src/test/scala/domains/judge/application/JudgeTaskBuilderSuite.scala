package domains.judge.application

import domains.problem.objects.response.ProblemDetail
import domains.problem.objects.{ProblemDataManifest, ProblemDataManifestEntry}
import domains.problem.objects.{OthersSubmissionAccess, ProblemData, ProblemDataPath, ProblemId, ProblemSlug, ProblemSpaceLimitMb, ProblemStatementText, ProblemTimeLimitMs, ProblemTitle}
import domains.user.objects.{DisplayName, UserIdentity, Username}
import domains.submission.objects.{SubmissionId, SubmissionLanguage, SubmissionSourceCode}
import domains.submission.objects.internal.ClaimedSubmission
import munit.FunSuite
import shared.objects.access.{BaseAccess, ResourceAccessPolicy}

import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.UUID

class JudgeTaskBuilderSuite extends FunSuite:

  private val claimedSubmission = ClaimedSubmission(
    id = SubmissionId(1),
    problemId = ProblemId(UUID.fromString("11111111-1111-4111-8111-111111111111")),
    problemSlug = ProblemSlug("sample-problem"),
    language = SubmissionLanguage.Cpp17,
    sourceCode = SubmissionSourceCode("int main() {}"),
    timeLimitMs = ProblemTimeLimitMs(1000),
    spaceLimitMb = ProblemSpaceLimitMb(256)
  )

  private val manifest = ProblemDataManifest.fromEntries(
    claimedSubmission.problemSlug,
    List(
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
    timeLimitMs = ProblemTimeLimitMs(1000),
    spaceLimitMb = ProblemSpaceLimitMb(256),
    accessPolicy = ResourceAccessPolicy(BaseAccess.OwnerOnly, Nil, Nil),
    othersSubmissionAccess = OthersSubmissionAccess.None,
    creator = UserIdentity(Username("owner"), DisplayName("Owner")),
    canManage = true,
    createdAt = Instant.EPOCH,
    updatedAt = Instant.EPOCH
  )

  test("parseConfigBytes accepts enum aggregation names") {
    val result = JudgeTaskBuilder.parseConfigBytes(
      yaml("""
        |version: 1
        |limits:
        |  timeMs: 1000
        |  memoryMb: 256
        |checker:
        |  type: builtin
        |  name: exact
        |aggregation:
        |  testcases: sum_max_max
        |  subtasks: sum_max_max
        |subtasks:
        |  - name: sample
        |    testcases:
        |      - input: sample/1.in
        |        answer: sample/1.ans
        |"""),
      claimedSubmission,
      manifest
    )

    assert(result.isRight)
    result.foreach { task =>
      assertEquals(task.aggregation.score, "sum")
      assertEquals(task.aggregation.time, "max")
      assertEquals(task.aggregation.memory, "max")
      assertEquals(task.subtasks.head.aggregation.score, "sum")
      assertEquals(task.subtasks.head.aggregation.time, "max")
      assertEquals(task.subtasks.head.aggregation.memory, "max")
    }
  }

  test("parseConfigBytes rejects old comma aggregation values") {
    val result = JudgeTaskBuilder.parseConfigBytes(
      yaml("""
        |version: 1
        |limits:
        |  timeMs: 1000
        |  memoryMb: 256
        |checker:
        |  type: builtin
        |  name: exact
        |aggregation:
        |  testcases: sum,max,max
        |subtasks:
        |  - testcases:
        |      - answer: sample/1.ans
        |"""),
      claimedSubmission,
      manifest
    )

    assertEquals(
      result.left.toOption,
      Some("Unsupported aggregation: sum,max,max. Expected one of: min_max_max, min_sum_max, sum_max_max, sum_sum_max.")
    )
  }

  test("parseConfigBytes uses claimed submission limits when config omits limits") {
    val result = JudgeTaskBuilder.parseConfigBytes(
      yaml("""
        |version: 1
        |checker:
        |  type: builtin
        |  name: exact
        |subtasks:
        |  - name: sample
        |    testcases:
        |      - input: sample/1.in
        |        answer: sample/1.ans
        |"""),
      claimedSubmission,
      manifest
    )

    assertEquals(result.map(_.subtasks.head.testcases.head.limits.timeMs.value), Right(1000))
    assertEquals(result.map(_.subtasks.head.testcases.head.limits.memoryMb.value), Right(256))
  }

  test("validateReadyConfigBytes returns judge.yaml and referenced file paths") {
    val result = JudgeTaskBuilder.validateReadyConfigBytes(
      yaml("""
        |version: 1
        |limits:
        |  timeMs: 1000
        |  memoryMb: 256
        |checker:
        |  type: builtin
        |  name: exact
        |subtasks:
        |  - name: sample
        |    testcases:
        |      - input: sample/1.in
        |        answer: sample/1.ans
        |"""),
      problem,
      manifest
    )

    assertEquals(
      result.map(_.retainedPaths),
      Right(Set(ProblemDataPath("judge.yaml"), ProblemDataPath("sample/1.in"), ProblemDataPath("sample/1.ans")))
    )
  }

  private def entry(path: String): ProblemDataManifestEntry =
    ProblemDataManifestEntry(ProblemDataPath(path), sizeBytes = 1L, sha256 = path)

  private def yaml(content: String): Array[Byte] =
    content.stripMargin.trim.getBytes(StandardCharsets.UTF_8)
