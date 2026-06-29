import type { ProblemId } from '@/objects/problem/ProblemId'
import type { ProblemSlug } from '@/objects/problem/ProblemSlug'
import type { SubmissionId } from '@/objects/submission/SubmissionId'
import type { SubmissionLanguage } from '@/objects/submission/SubmissionLanguage'

/** 已领取提交中的单个程序清单；供 judger 下载源文件并识别语言。 */
type ClaimedSubmissionProgram = {
  language: SubmissionLanguage
  sourceKey: string
  sizeBytes: number
  sha256: string
}

/** 已领取提交的程序清单；defaultProgramKey 指向主程序。 */
type ClaimedSubmissionProgramManifest = {
  defaultProgramKey: string
  programs: Record<string, ClaimedSubmissionProgram>
}

/** 判题 worker 领取到的提交任务；包含题目标识和程序清单，不含判题结果。 */
export type ClaimedSubmission = {
  id: SubmissionId
  problemId: ProblemId
  problemSlug: ProblemSlug
  programManifest: ClaimedSubmissionProgramManifest
}
