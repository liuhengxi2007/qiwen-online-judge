package domains.submission.objects.response

import domains.submission.objects.*

import domains.user.objects.UserIdentity
import domains.problem.objects.{ProblemId, ProblemSlug, ProblemTitle}
import judgeprotocol.model.JudgeResult

import java.time.Instant

final case class SubmissionDetail(
  id: SubmissionId,
  problemId: ProblemId,
  problemSlug: ProblemSlug,
  problemTitle: ProblemTitle,
  canManage: Boolean,
  submitter: UserIdentity,
  language: SubmissionLanguage,
  status: SubmissionStatus,
  verdict: Option[SubmissionVerdict],
  judgeMessage: Option[String],
  timeUsedMs: Option[Long],
  memoryUsedKb: Option[Long],
  score: Option[BigDecimal],
  judgeResult: Option[JudgeResult],
  codeLength: Int,
  sourceCode: SubmissionSourceCode,
  submittedAt: Instant,
  startedAt: Option[Instant],
  finishedAt: Option[Instant]
)
