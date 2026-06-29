/** 提交源代码品牌类型；保留原始内容，不自动 trim 通过后的代码。 */
export type SubmissionSourceCode = string & { readonly __brand: 'SubmissionSourceCode' }

/** 解析 helper 的返回结果；成功携带品牌值，失败携带可展示错误，不抛异常。 */
type ParseResult<T> = { ok: true; value: T } | { ok: false; error: string }

/** 创建提交源代码品牌值；调用前必须完成非空和长度校验。 */
function createSubmissionSourceCode(value: string): SubmissionSourceCode {
  /** 注意：这里的 as 只在 parseSubmissionSourceCode 校验通过后施加品牌类型。 */
  return value as SubmissionSourceCode
}

/** 将提交源代码品牌值还原为字符串；调用方需避免日志输出完整代码。 */
export function submissionSourceCodeValue(sourceCode: SubmissionSourceCode): string {
  return sourceCode
}

/** 解析提交源代码；拒绝空白代码和超长代码，但保留原始文本内容。 */
export function parseSubmissionSourceCode(rawSourceCode: string): ParseResult<SubmissionSourceCode> {
  if (!rawSourceCode.trim()) {
    return { ok: false, error: 'Source code is required.' }
  }

  if (rawSourceCode.length > 200_000) {
    return { ok: false, error: 'Source code must be at most 200000 characters.' }
  }

  return { ok: true, value: createSubmissionSourceCode(rawSourceCode) }
}
