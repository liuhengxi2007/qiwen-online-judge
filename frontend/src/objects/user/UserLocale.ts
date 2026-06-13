/** 用户界面语言偏好；当前支持英文和简体中文。 */
export type UserLocale = 'en' | 'zh-CN'

/** 解析 helper 的返回结果；成功携带品牌值，失败携带可展示错误，不抛异常。 */
type ParseResult<T> = { ok: true; value: T } | { ok: false; error: string }

/** 返回语言偏好原始枚举值；用于偏好请求和本地展示。 */
export function userLocaleValue(locale: UserLocale): UserLocale {
  return locale
}

/** 解析语言偏好字符串；拒绝未支持的 locale。 */
export function parseUserLocale(rawLocale: string): ParseResult<UserLocale> {
  const normalized = rawLocale.trim()

  switch (normalized) {
    case 'en':
    case 'zh-CN':
      return { ok: true, value: normalized }
    default:
      return {
        ok: false,
        error: 'Locale must be one of: en, zh-CN.',
      }
  }
}
