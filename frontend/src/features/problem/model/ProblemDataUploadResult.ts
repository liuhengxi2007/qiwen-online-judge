import type { ProblemDetail } from '@/features/problem/model/ProblemDetail'

export type ProblemDataUploadResult = {
  problem: ProblemDetail
  uploadedFileCount: number
}
