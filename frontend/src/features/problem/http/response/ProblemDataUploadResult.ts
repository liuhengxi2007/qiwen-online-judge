import type { ProblemDetail } from '@/features/problem/http/response/ProblemDetail'

export type ProblemDataUploadResult = {
  problem: ProblemDetail
  uploadedFileCount: number
}
