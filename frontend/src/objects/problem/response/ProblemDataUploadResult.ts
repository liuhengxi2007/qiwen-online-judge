import type { ProblemDetail } from '@/objects/problem/response/ProblemDetail'

/** 题目数据上传结果；返回更新后的题目详情和本次接收的文件数。 */
export type ProblemDataUploadResult = {
  problem: ProblemDetail
  uploadedFileCount: number
}
