package domains.submission.objects.response

import domains.submission.objects.*

import domains.user.objects.UserIdentity
import domains.problem.objects.{ProblemId, ProblemSlug, ProblemTitle}

import java.time.Instant

final case class SubmissionSummary(
  id: SubmissionId,
  problemId: ProblemId,
  problemSlug: ProblemSlug,
  problemTitle: ProblemTitle,
  canViewDetail: Boolean,
  submitter: UserIdentity,
  language: SubmissionLanguage,
  status: SubmissionStatus,
  verdict: Option[SubmissionVerdict],
  timeUsedMs: Option[Long],
  memoryUsedKb: Option[Long],
  score: Option[BigDecimal],
  codeLength: Int,
  submittedAt: Instant,
  startedAt: Option[Instant],
  finishedAt: Option[Instant]
)
