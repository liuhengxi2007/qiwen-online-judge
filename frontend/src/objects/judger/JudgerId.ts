/** judger 租约标识；管理端 API 用于展示已注册 worker。 */
export type JudgerId = string & { readonly __brand: 'JudgerId' }

/** 解析 helper 的返回结果；成功携带品牌值，失败携带可展示错误，不抛异常。 */
type ParseResult<T> = { ok: true; value: T } | { ok: false; error: string }

/** 创建 judger id 品牌值；调用前必须完成空值和长度校验。 */
function createJudgerId(value: string): JudgerId {
  /** 注意：这里的 as 只在 parseJudgerId 校验通过后施加品牌类型。 */
  return value as JudgerId
}

/** 将 judger id 品牌值还原为普通字符串；用于展示、key 和 API body。 */
export function judgerIdValue(judgerId: JudgerId): string {
  return judgerId
}

/** 解析 judger id；拒绝空值和过长值，规则与 worker 协议保持一致。 */
export function parseJudgerId(rawJudgerId: string): ParseResult<JudgerId> {
  const normalized = rawJudgerId.trim()

  if (!normalized) {
    return { ok: false, error: 'Judger id is required.' }
  }

  if (normalized.length > 120) {
    return { ok: false, error: 'Judger id must be at most 120 characters.' }
  }

  return { ok: true, value: createJudgerId(normalized) }
}
