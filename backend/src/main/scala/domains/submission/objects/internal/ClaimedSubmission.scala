package domains.submission.objects.internal

import domains.problem.objects.{ProblemId, ProblemSlug}
import domains.submission.objects.SubmissionId

/** 提交领取任务载荷，由 ClaimNextJudgeSubmission/SubmissionJudgeTable 返回，并供 ClaimJudgeTask、JudgeTaskBuilder 和 hack 判题复用。 */
final case class ClaimedSubmission(
  id: SubmissionId,
  problemId: ProblemId,
  problemSlug: ProblemSlug,
  programManifest: SubmissionProgramManifest
)
