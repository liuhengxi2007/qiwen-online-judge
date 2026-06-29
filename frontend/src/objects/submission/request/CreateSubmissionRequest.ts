import type { ProblemSlug } from '@/objects/problem/ProblemSlug'
import type { SubmissionLanguage } from '@/objects/submission/SubmissionLanguage'
import type { SubmissionSourceCode } from '@/objects/submission/SubmissionSourceCode'

/** 创建提交请求中的程序定义；key 由 Record 外层提供。 */
type CreateSubmissionProgram = {
  language: SubmissionLanguage
  sourceCode: SubmissionSourceCode
}

/** 创建提交请求体；支持多程序提交，programs 的 key 由前端表单定义。 */
export type CreateSubmissionRequest = {
  problemSlug: ProblemSlug
  programs: Record<string, CreateSubmissionProgram>
}
