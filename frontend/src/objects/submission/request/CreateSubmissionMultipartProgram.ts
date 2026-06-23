import type { SubmissionLanguage } from '@/objects/submission/SubmissionLanguage'

/** multipart 提交中的程序元数据；sourcePart 指向 FormData 中对应源码字段。 */
/** 注意：role 和 sourcePart 是动态 multipart 协议字段，后端按字符串校验去重，不是固定状态枚举。 */
export type CreateSubmissionMultipartProgram = {
  role: string
  language: SubmissionLanguage
  sourcePart: string
}
