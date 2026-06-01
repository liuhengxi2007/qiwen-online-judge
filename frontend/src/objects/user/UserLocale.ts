export type UserLocale = 'en' | 'zh-CN'

type ParseResult<T> = { ok: true; value: T } | { ok: false; error: string }

export function userLocaleValue(locale: UserLocale): UserLocale {
  return locale
}

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

export function fromUserLocaleContract(value: string, label: string): UserLocale {
  const result = parseUserLocale(value)
  if (!result.ok) {
    throw new Error(`Invalid ${label} in contract payload: ${result.error}`)
  }

  return result.value
}

export function toUserLocaleContract(value: UserLocale): UserLocale {
  return userLocaleValue(value)
}
