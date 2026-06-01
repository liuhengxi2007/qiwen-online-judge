import type { JudgeResult } from '@/objects/submission/JudgeResult'
import { fromJudgeResultContract } from '@/objects/submission/JudgeResult'
import type { SubmissionStatus } from '@/objects/submission/SubmissionStatus'
import { fromSubmissionStatusContract } from '@/objects/submission/SubmissionStatus'
import { readNullable, readRecord, readString } from '@/objects/shared/PageResponse'

export type SubmissionJudgeState = {
  status: SubmissionStatus
  judgeResult: JudgeResult | null
  startedAt: string | null
  finishedAt: string | null
}

export function fromSubmissionJudgeStateContract(value: unknown, label = 'submission judge state'): SubmissionJudgeState {
  const state = readRecord(value, label)
  return {
    status: fromSubmissionStatusContract(state.status),
    judgeResult: readNullable(state.judgeResult, `${label} judge result`, fromJudgeResultContract),
    startedAt: readNullable(state.startedAt, `${label} started at`, readString),
    finishedAt: readNullable(state.finishedAt, `${label} finished at`, readString),
  }
}
