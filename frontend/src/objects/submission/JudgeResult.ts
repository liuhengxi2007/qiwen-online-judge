import type { JudgeFailureReason } from '@/objects/submission/JudgeFailureReason'
import { fromJudgeFailureReasonContract } from '@/objects/submission/JudgeFailureReason'
import type { JudgeSubtaskResult } from '@/objects/submission/JudgeSubtaskResult'
import { fromJudgeSubtaskResultContract } from '@/objects/submission/JudgeSubtaskResult'
import type { SubmissionVerdict } from '@/objects/submission/SubmissionVerdict'
import { fromSubmissionVerdictContract } from '@/objects/submission/SubmissionVerdict'
import { readArray, readNonNegativeSafeInteger, readNullable, readNumber, readRecord } from '@/objects/shared/PageResponse'

export type JudgeResult = {
  score: number
  verdict: SubmissionVerdict
  reason: JudgeFailureReason | null
  timeUsedMs: number | null
  memoryUsedKb: number | null
  subtasks: JudgeSubtaskResult[]
}

export function fromJudgeResultContract(value: unknown, label: string): JudgeResult {
  const result = readRecord(value, label)
  return {
    score: readNumber(result.score, `${label} score`),
    verdict: fromSubmissionVerdictContract(result.verdict),
    reason: readNullable(result.reason, `${label} reason`, fromJudgeFailureReasonContract),
    timeUsedMs: readNullable(result.timeUsedMs, `${label} time used ms`, readNonNegativeSafeInteger),
    memoryUsedKb: readNullable(result.memoryUsedKb, `${label} memory used kb`, readNonNegativeSafeInteger),
    subtasks: readArray(result.subtasks, `${label} subtasks`, fromJudgeSubtaskResultContract),
  }
}
