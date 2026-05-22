package domains.submission.application.output

import domains.submission.model.*

import domains.user.model.UserIdentity
import domains.problem.model.{ProblemId, ProblemSlug, ProblemTitle}
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
