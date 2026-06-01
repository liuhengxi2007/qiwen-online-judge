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