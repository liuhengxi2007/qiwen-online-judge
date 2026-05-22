package domains.submission.application.output

import domains.submission.model.*

import domains.user.model.UserIdentity
import domains.problem.model.{ProblemId, ProblemSlug, ProblemTitle}

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
