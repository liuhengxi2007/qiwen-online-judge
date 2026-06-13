package domains.submission.objects.internal

import domains.problem.objects.{ProblemId, ProblemSlug}
import domains.submission.objects.SubmissionId

/** 已被 worker 领取的提交任务；包含构建 JudgeTask 所需的题目和程序 manifest。 */
final case class ClaimedSubmission(
  id: SubmissionId,
  problemId: ProblemId,
  problemSlug: ProblemSlug,
  programManifest: SubmissionProgramManifest
)
