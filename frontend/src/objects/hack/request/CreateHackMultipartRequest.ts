import type { SubmissionId } from '@/objects/submission/SubmissionId'

/** multipart 创建 Hack 请求；input/strategyProviderSource 可用文本或文件传输。 */
export type CreateHackMultipartRequest = {
  targetSubmissionId: SubmissionId
  subtaskIndex: number
  input: { kind: 'text'; value: string } | { kind: 'file'; value: File }
  strategyProviderSource?: { kind: 'text'; value: string } | { kind: 'file'; value: File } | null
}
