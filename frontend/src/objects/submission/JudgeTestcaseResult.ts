import type { JudgeFailureReason } from '@/objects/submission/JudgeFailureReason'
import { fromJudgeFailureReasonContract } from '@/objects/submission/JudgeFailureReason'
import type { SubmissionVerdict } from '@/objects/submission/SubmissionVerdict'
import { fromSubmissionVerdictContract } from '@/objects/submission/SubmissionVerdict'
import { readNonNegativeSafeInteger, readNullable, readNumber, readRecord, readString } from '@/objects/shared/PageResponse'

export type JudgeTestcaseResult = {
  name: string
  score: number
  verdict: SubmissionVerdict
  message: string | null
  reason: JudgeFailureReason | null
  timeUsedMs: number | null
  memoryUsedKb: number | null
}

export function fromJudgeTestcaseResultContract(value: unknown, label: string): JudgeTestcaseResult {
  const result = readRecord(value, label)
  return {
    name: readString(result.name, `${label} name`),
    score: readNumber(result.score, `${label} score`),
    verdict: fromSubmissionVerdictContract(result.verdict),
    message: readNullable(result.message, `${label} message`, readString),
    reason: readNullable(result.reason, `${label} reason`, fromJudgeFailureReasonContract),
    timeUsedMs: readNullable(result.timeUsedMs, `${label} time used ms`, readNonNegativeSafeInteger),
    memoryUsedKb: readNullable(result.memoryUsedKb, `${label} memory used kb`, readNonNegativeSafeInteger),
  }
}
