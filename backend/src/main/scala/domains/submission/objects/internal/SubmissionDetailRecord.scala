package domains.submission.objects.internal

import domains.problem.objects.{ProblemId, ProblemSlug, ProblemTitle}
import domains.submission.objects.{SubmissionId, SubmissionLanguage, SubmissionResultDisplayMode, SubmissionSource, SubmissionStatus, SubmissionVerdict}
import domains.user.objects.UserIdentity
import judgeprotocol.objects.response.JudgeResult

import java.time.Instant

/** 提交详情的数据库内部记录；包含程序 manifest 但不包含实际源码文本。 */
final case class SubmissionDetailRecord(
  id: SubmissionId,
  problemId: ProblemId,
  problemSlug: ProblemSlug,
  problemTitle: ProblemTitle,
  resultDisplayMode: SubmissionResultDisplayMode,
  source: SubmissionSource,
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
)
