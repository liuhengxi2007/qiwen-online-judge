import type { UserIdentity } from '@/objects/user/UserIdentity'
import type { ProblemId } from '@/objects/problem/ProblemId'
import type { ProblemSlug } from '@/objects/problem/ProblemSlug'
import type { ProblemTitle } from '@/objects/problem/ProblemTitle'
import type { SubmissionId } from '@/objects/submission/SubmissionId'
import type { SubmissionLanguage } from '@/objects/submission/SubmissionLanguage'
import type { SubmissionSourceCode } from '@/objects/submission/SubmissionSourceCode'
import type { SubmissionStatus } from '@/objects/submission/SubmissionStatus'
import type { SubmissionVerdict } from '@/objects/submission/SubmissionVerdict'
import type { JudgeResult } from '@/objects/submission/JudgeResult'
import type { SubmissionSource } from '@/objects/submission/SubmissionSource'
import type { SubmissionResultDisplayMode } from '@/objects/submission/SubmissionResultDisplayMode'

type SubmissionDetailProgram = {
  language: SubmissionLanguage
  sourceCode: SubmissionSourceCode
}

export type SubmissionDetail = {
  id: SubmissionId
  problemId: ProblemId
  problemSlug: ProblemSlug
  problemTitle: ProblemTitle
  resultDisplayMode: SubmissionResultDisplayMode
  source: SubmissionSource
  canManage: boolean
  submitter: UserIdentity
  language: SubmissionLanguage
  status: SubmissionStatus
  verdict: SubmissionVerdict | null
  timeUsedMs: number | null
  memoryUsedKb: number | null
  score: number | null
  judgeResult: JudgeResult | null
  codeLength: number
  sourceCode: SubmissionSourceCode
  programs: Record<string, SubmissionDetailProgram>
  submittedAt: string
  startedAt: string | null
  finishedAt: string | null
}
