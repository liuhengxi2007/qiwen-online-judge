import type { ProblemSlug } from '@/objects/problem/ProblemSlug'
import type { CreateSubmissionMultipartProgram } from '@/objects/submission/request/CreateSubmissionMultipartProgram'

/** multipart 创建提交请求；包含题目 slug、程序元数据和源码片段集合。 */
export type CreateSubmissionMultipartRequest = {
  problemSlug: ProblemSlug
  programs: CreateSubmissionMultipartProgram[]
  /** multipart 提交中的源码片段；source 可为文本或文件。 */
  sources: Array<{
    sourcePart: string
    source: string | File
  }>
}
