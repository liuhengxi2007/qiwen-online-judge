package domains.submission.objects.response

import domains.contest.objects.{ContestSlug, ContestTitle}
import domains.problem.objects.{ProblemId, ProblemSlug, ProblemTitle}
import domains.submission.objects.*
import domains.user.objects.{DisplayName, UserIdentity, Username}
import io.circe.parser.decode
import io.circe.syntax.*
import munit.FunSuite

import java.time.Instant
import java.util.UUID

class SubmissionResponseCodecSuite extends FunSuite:

  private val submitter = UserIdentity(Username("alice"), DisplayName("Alice"))
  private val problemId = ProblemId(UUID.fromString("11111111-1111-4111-8111-111111111111"))

  test("SubmissionSummary encodes result display mode") {
    val summary = SubmissionSummary(
      id = SubmissionId(1),
      problemId = problemId,
      problemSlug = ProblemSlug("sample-problem"),
      problemTitle = ProblemTitle("Sample Problem"),
      resultDisplayMode = SubmissionResultDisplayMode.Verdict,
      source = SubmissionSource.fromContest(ContestSlug("sample-contest"), ContestTitle("Sample Contest")),
      canViewDetail = true,
      submitter = submitter,
      language = SubmissionLanguage.Cpp17,
      status = SubmissionStatus.Completed,
      verdict = Some(SubmissionVerdict.Accepted),
      timeUsedMs = Some(10L),
      memoryUsedKb = Some(1024L),
      score = Some(BigDecimal(1)),
      codeLength = 42,
      submittedAt = Instant.EPOCH,
      startedAt = None,
      finishedAt = None
    )
    val json = summary.asJson

    assertEquals(json.hcursor.downField("resultDisplayMode").as[String], Right("verdict"))
    assertEquals(decode[SubmissionSummary](json.noSpaces).map(_.resultDisplayMode), Right(SubmissionResultDisplayMode.Verdict))
  }

  test("SubmissionDetail encodes result display mode") {
    val detail = SubmissionDetail(
      id = SubmissionId(1),
      problemId = problemId,
      problemSlug = ProblemSlug("sample-problem"),
      problemTitle = ProblemTitle("Sample Problem"),
      resultDisplayMode = SubmissionResultDisplayMode.Score,
      source = SubmissionSource.FromProblemSet,
      canManage = false,
      submitter = submitter,
      language = SubmissionLanguage.Cpp17,
      status = SubmissionStatus.Completed,
      verdict = Some(SubmissionVerdict.Accepted),
      timeUsedMs = Some(10L),
      memoryUsedKb = Some(1024L),
      score = Some(BigDecimal(1)),
      judgeResult = None,
      codeLength = 42,
      sourceCode = SubmissionSourceCode("int main() {}"),
      programs = Map.empty,
      submittedAt = Instant.EPOCH,
      startedAt = None,
      finishedAt = None
    )
    val json = detail.asJson

    assertEquals(json.hcursor.downField("resultDisplayMode").as[String], Right("score"))
    assertEquals(decode[SubmissionDetail](json.noSpaces).map(_.resultDisplayMode), Right(SubmissionResultDisplayMode.Score))
  }
