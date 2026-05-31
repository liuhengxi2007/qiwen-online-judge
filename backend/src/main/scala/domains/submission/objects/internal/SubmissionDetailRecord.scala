package domains.submission.objects.internal

import domains.problem.objects.{ProblemId, ProblemSlug, ProblemTitle}
import domains.submission.objects.{SubmissionId, SubmissionLanguage, SubmissionSourceCode, SubmissionStatus, SubmissionVerdict}
import domains.submission.objects.response.SubmissionDetail
import domains.user.objects.UserIdentity
import judgeprotocol.objects.response.JudgeResult

import java.time.Instant

final case class SubmissionDetailRecord(
  id: SubmissionId,
  problemId: ProblemId,
  problemSlug: ProblemSlug,
  problemTitle: ProblemTitle,
  submitter: UserIdentity,
  language: SubmissionLanguage,
  status: SubmissionStatus,
  verdict: Option[SubmissionVerdict],
  timeUsedMs: Option[Long],
  memoryUsedKb: Option[Long],
  score: Option[BigDecimal],
  judgeResult: Option[JudgeResult],
  codeLength: Int,
  programManifest: SubmissionProgramManifest,
  submittedAt: Instant,
  startedAt: Option[Instant],
  finishedAt: Option[Instant]
):
  def toSubmissionDetail(sourceCode: SubmissionSourceCode, canManage: Boolean = false): SubmissionDetail =
    SubmissionDetail(
      id = id,
      problemId = problemId,
      problemSlug = problemSlug,
      problemTitle = problemTitle,
      canManage = canManage,
      submitter = submitter,
      language = language,
      status = status,
      verdict = verdict,
      timeUsedMs = timeUsedMs,
      memoryUsedKb = memoryUsedKb,
      score = score,
      judgeResult = judgeResult,
      codeLength = codeLength,
      sourceCode = sourceCode,
      submittedAt = submittedAt,
      startedAt = startedAt,
      finishedAt = finishedAt
    )
