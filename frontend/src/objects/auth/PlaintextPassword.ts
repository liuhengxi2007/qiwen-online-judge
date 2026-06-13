/** 明文密码品牌类型；仅用于登录/注册/改密请求，禁止持久化。 */
export type PlaintextPassword = string & { readonly __brand: 'PlaintextPassword' }

/** 解析 helper 的返回结果；成功携带品牌值，失败携带可展示错误，不抛异常。 */
type ParseResult<T> = { ok: true; value: T } | { ok: false; error: string }

/** 创建明文密码品牌值；调用前必须确认非空。 */
function createPlaintextPassword(value: string): PlaintextPassword {
  /** 注意：这里的 as 只在 parsePlaintextPassword 校验通过后施加品牌类型。 */
  return value as PlaintextPassword
}

/** 将明文密码品牌值还原为 API 请求字符串；调用方需避免日志输出。 */
export function plaintextPasswordValue(password: PlaintextPassword): string {
  return password
}

/** 解析明文密码输入；当前仅做非空校验并返回结构化失败原因。 */
export function parsePlaintextPassword(rawPassword: string): ParseResult<PlaintextPassword> {
  const normalized = rawPassword.trim()

  if (!normalized) {
    return { ok: false, error: 'Password is required.' }
  }

  return { ok: true, value: createPlaintextPassword(normalized) }
}
