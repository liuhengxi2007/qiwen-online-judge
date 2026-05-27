import { describe, expect, it } from 'vitest'

import { parseEmailAddress } from '@/objects/auth/EmailAddress'
import { parsePlaintextPassword } from '@/objects/auth/PlaintextPassword'

describe('auth-parsers', () => {
  it('rejects invalid emails and passwords', () => {
    expect(parseEmailAddress('   ')).toEqual({
      ok: false,
      error: 'Email is required.',
    })
    expect(parseEmailAddress('not-an-email')).toEqual({
      ok: false,
      error: 'Please enter a valid email address.',
    })
    expect(parseEmailAddress(`${'a'.repeat(250)}@x.com`)).toEqual({
      ok: false,
      error: 'Email must be at most 255 characters.',
    })
    expect(parsePlaintextPassword('   ')).toEqual({
      ok: false,
      error: 'Password is required.',
    })
  })

  it('parses trimmed valid email addresses and passwords', () => {
    expect(parseEmailAddress('  alice@example.com  ')).toEqual({
      ok: true,
      value: 'alice@example.com',
    })
    expect(parsePlaintextPassword('  secret  ')).toEqual({
      ok: true,
      value: 'secret',
    })
  })
})
