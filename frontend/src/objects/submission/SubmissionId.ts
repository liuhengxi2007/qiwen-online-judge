/** 提交 ID 品牌类型；表示后端分配的正整数提交标识。 */
export type SubmissionId = number & { readonly __brand: 'SubmissionId' }

/** 解析 helper 的返回结果；成功携带品牌值，失败携带可展示错误，不抛异常。 */
type ParseResult<T> = { ok: true; value: T } | { ok: false; error: string }

/** 创建提交 ID 品牌值；调用前必须确认是正安全整数。 */
function createSubmissionId(value: number): SubmissionId {
  /** 注意：这里的 as 只在 parseSubmissionId 校验通过后施加品牌类型。 */
  return value as SubmissionId
}

/** 将提交 ID 品牌值还原为数字；用于 API path/body。 */
export function submissionIdValue(submissionId: SubmissionId): number {
  return submissionId
}

/** 解析提交 ID；拒绝非整数和小于 1 的值。 */
export function parseSubmissionId(rawId: number): ParseResult<SubmissionId> {
  if (!Number.isSafeInteger(rawId)) {
    return { ok: false, error: 'Submission id must be an integer.' }
  }

  if (rawId < 1) {
    return { ok: false, error: 'Submission id is required.' }
  }

  return { ok: true, value: createSubmissionId(rawId) }
}
