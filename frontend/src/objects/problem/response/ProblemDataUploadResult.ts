import type { ProblemDetail } from '@/objects/problem/response/ProblemDetail'

export type ProblemDataUploadResult = {
  problem: ProblemDetail
  uploadedFileCount: number
}
