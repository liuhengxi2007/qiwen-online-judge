import { beforeEach, describe, expect, it } from 'vitest'

import { readAuthSession } from './AuthSessionStorage'
import { toAuthSession } from './AuthSession'

const rawSession = {
  displayName: 'Alice',
  username: 'alice',
  avatarUrl: null,
  email: 'alice@example.com',
  preferences: {
    displayMode: 'display_name',
    locale: 'en',
    problemTitleDisplayMode: 'title',
    autoMarkMessageRead: false,
  },
  siteManager: true,
  problemManager: false,
  contestManager: false,
}

describe('auth-session', () => {
  beforeEach(() => {
    window.localStorage.clear()
  })

  it('normalizes site manager permissions from API responses', () => {
    expect(toAuthSession(rawSession as Parameters<typeof toAuthSession>[0])).toMatchObject({
      siteManager: true,
      problemManager: true,
      contestManager: true,
    })
  })

  it('normalizes site manager permissions from local storage', () => {
    window.localStorage.setItem('auth_session', JSON.stringify(rawSession))

    expect(readAuthSession()).toMatchObject({
      siteManager: true,
      problemManager: true,
      contestManager: true,
    })
  })
})
