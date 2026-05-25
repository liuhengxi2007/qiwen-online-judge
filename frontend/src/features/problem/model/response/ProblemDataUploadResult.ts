import type { ProblemDetail } from '@/features/problem/model/response/ProblemDetail'

export type ProblemDataUploadResult = {
  problem: ProblemDetail
  uploadedFileCount: number
}
