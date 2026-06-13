/** Hack 记录 ID 品牌类型；表示后端分配的正整数标识。 */
export type HackId = number & { readonly __brand: 'HackId' }

/** 解析 helper 的返回结果；成功携带品牌值，失败携带可展示错误，不抛异常。 */
type ParseResult<T> = { ok: true; value: T } | { ok: false; error: string }

/** 创建 Hack ID 品牌值；调用前必须确认是正安全整数。 */
function createHackId(value: number): HackId {
  /** 注意：这里的 as 只在 parseHackId 校验通过后施加品牌类型。 */
  return value as HackId
}

/** 将 Hack ID 品牌值还原为数字；用于 API path/body。 */
export function hackIdValue(hackId: HackId): number {
  return hackId
}

/** 解析 Hack ID；拒绝非整数和小于 1 的值。 */
export function parseHackId(rawId: number): ParseResult<HackId> {
  if (!Number.isSafeInteger(rawId)) {
    return { ok: false, error: 'Hack id must be an integer.' }
  }

  if (rawId < 1) {
    return { ok: false, error: 'Hack id is required.' }
  }

  return { ok: true, value: createHackId(rawId) }
}
