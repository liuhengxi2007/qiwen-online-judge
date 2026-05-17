package domains.judge.application

import domains.problem.application.{ProblemDataManifest, ProblemDataManifestEntry}
import domains.problem.model.{ProblemDataPath, ProblemId, ProblemSlug}
import domains.submission.model.{SubmissionId, SubmissionLanguage, SubmissionSourceCode}
import domains.submission.table.ClaimedSubmission
import munit.FunSuite

import java.nio.charset.StandardCharsets
import java.util.UUID

class JudgeTaskBuilderSuite extends FunSuite:

  private val claimedSubmission = ClaimedSubmission(
    id = SubmissionId(1),
    problemId = ProblemId(UUID.fromString("11111111-1111-4111-8111-111111111111")),
    problemSlug = ProblemSlug("sample-problem"),
    language = SubmissionLanguage.Cpp17,
    sourceCode = SubmissionSourceCode("int main() {}"),
    timeLimitMs = 1000,
    spaceLimitMb = 256
  )

  private val manifest = ProblemDataManifest.fromEntries(
    claimedSubmission.problemSlug,
    List(
      entry("sample/1.in"),
      entry("sample/1.ans")
    )
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

  private def entry(path: String): ProblemDataManifestEntry =
    ProblemDataManifestEntry(ProblemDataPath(path), sizeBytes = 1L, sha256 = path)

  private def yaml(content: String): Array[Byte] =
    content.stripMargin.trim.getBytes(StandardCharsets.UTF_8)
