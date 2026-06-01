export type EmailAddress = string & { readonly __brand: 'EmailAddress' }

type ParseResult<T> = { ok: true; value: T } | { ok: false; error: string }

const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

function createEmailAddress(value: string): EmailAddress {
  return value as EmailAddress
}

export function emailAddressValue(emailAddress: EmailAddress): string {
  return emailAddress
}

export function parseEmailAddress(rawEmailAddress: string): ParseResult<EmailAddress> {
  const normalized = rawEmailAddress.trim()

  if (!normalized) {
    return { ok: false, error: 'Email is required.' }
  }

  if (normalized.length > 255) {
    return { ok: false, error: 'Email must be at most 255 characters.' }
  }

  if (!emailPattern.test(normalized)) {
    return { ok: false, error: 'Please enter a valid email address.' }
  }

  return { ok: true, value: createEmailAddress(normalized) }
}