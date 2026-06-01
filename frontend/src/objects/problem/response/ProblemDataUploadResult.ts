import type { ProblemDetail } from '@/objects/problem/response/ProblemDetail'
import { fromProblemDetailContract } from '@/objects/problem/response/ProblemDetail'
import { readNonNegativeSafeInteger, readRecord } from '@/objects/shared/PageResponse'

export type ProblemDataUploadResult = {
  problem: ProblemDetail
  uploadedFileCount: number
}

export function fromProblemDataUploadResultContract(
  value: unknown,
  label = 'problem data upload result',
): ProblemDataUploadResult {
  const response = readRecord(value, label)
  return {
    problem: fromProblemDetailContract(response.problem, `${label} problem`),
    uploadedFileCount: readNonNegativeSafeInteger(response.uploadedFileCount, `${label} uploaded file count`),
  }
}
