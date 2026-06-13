/** 提交语言枚举；与后端和判题 worker 支持语言保持一致。 */
export type SubmissionLanguage = 'cpp17' | 'python3' | 'text'

const supportedSubmissionLanguages = ['cpp17', 'python3', 'text'] as const satisfies readonly SubmissionLanguage[]

/** 判断字符串是否为受支持提交语言；用于用户输入或响应窄化。 */
export function isSubmissionLanguage(value: string): value is SubmissionLanguage {
  /** 注意：includes 需要把待测字符串临时断言为枚举联合类型，不会改变运行时值。 */
  return supportedSubmissionLanguages.includes(value as SubmissionLanguage)
}
