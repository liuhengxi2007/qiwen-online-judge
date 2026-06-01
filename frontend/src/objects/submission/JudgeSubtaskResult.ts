import type { JudgeFailureReason } from '@/objects/submission/JudgeFailureReason'
import { fromJudgeFailureReasonContract } from '@/objects/submission/JudgeFailureReason'
import type { JudgeTestcaseResult } from '@/objects/submission/JudgeTestcaseResult'
import { fromJudgeTestcaseResultContract } from '@/objects/submission/JudgeTestcaseResult'
import type { SubmissionVerdict } from '@/objects/submission/SubmissionVerdict'
import { fromSubmissionVerdictContract } from '@/objects/submission/SubmissionVerdict'
import { readArray, readNonNegativeSafeInteger, readNullable, readNumber, readRecord, readString } from '@/objects/shared/PageResponse'

export type JudgeSubtaskResult = {
  name: string
  score: number
  verdict: SubmissionVerdict
  timeUsedMs: number | null
  memoryUsedKb: number | null
  reason: JudgeFailureReason | null
  testcases: JudgeTestcaseResult[]
}

export function fromJudgeSubtaskResultContract(value: unknown, label: string): JudgeSubtaskResult {
  const result = readRecord(value, label)
  return {
    name: readString(result.name, `${label} name`),
    score: readNumber(result.score, `${label} score`),
    verdict: fromSubmissionVerdictContract(result.verdict),
    timeUsedMs: readNullable(result.timeUsedMs, `${label} time used ms`, readNonNegativeSafeInteger),
    memoryUsedKb: readNullable(result.memoryUsedKb, `${label} memory used kb`, readNonNegativeSafeInteger),
    reason: readNullable(result.reason, `${label} reason`, fromJudgeFailureReasonContract),
    testcases: readArray(result.testcases, `${label} testcases`, fromJudgeTestcaseResultContract),
  }
}
