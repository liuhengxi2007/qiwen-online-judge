import { MemoryRouter } from 'react-router-dom'
import { cleanup, render, screen, within } from '@testing-library/react'
import { afterEach, describe, expect, it, vi } from 'vitest'

import type { SessionResponse } from '@/objects/auth/response/SessionResponse'
import { useAuthStore } from '@/pages/stores/auth/UseAuthStore'
import { I18nContext } from '@/system/i18n/i18n-context'
import { AppSectionBar } from './AppSectionBar'

const session: SessionResponse = {
  displayName: 'Alice' as SessionResponse['displayName'],
  username: 'alice' as SessionResponse['username'],
  avatarUrl: null,
  email: 'alice@example.com' as SessionResponse['email'],
  preferences: {
    displayMode: 'display_name',
    locale: 'en',
    problemTitleDisplayMode: 'title',
    autoMarkMessageRead: false,
  },
  siteManager: false,
  problemManager: false,
  contestManager: false,
}

function renderSectionBar() {
  return render(
    <I18nContext.Provider
      value={{
        locale: 'en',
        setLocale: vi.fn(),
        t: (key: string) => key,
      }}
    >
      <MemoryRouter>
        <AppSectionBar />
      </MemoryRouter>
    </I18nContext.Provider>,
  )
}

afterEach(() => {
  cleanup()
  useAuthStore.setState({ session: null })
  window.localStorage.clear()
})

describe('AppSectionBar', () => {
  it('does not render the app navigation without a session', () => {
    renderSectionBar()

    expect(screen.queryByRole('navigation')).toBeNull()
  })

  it('renders dashboard and grouped public navigation entries', () => {
    useAuthStore.setState({ session })

    renderSectionBar()

    const navigation = screen.getByRole('navigation')
    const links = within(navigation).getAllByRole('link')

    expect(links.map((link) => [link.textContent, link.getAttribute('href')])).toEqual([
      ['nav.dashboard', '/'],
      ['nav.problems', '/problems'],
      ['nav.submissions', '/submissions'],
      ['nav.hacks', '/hacks'],
      ['nav.problemSets', '/problem-sets'],
      ['nav.blogs', '/blogs'],
      ['nav.contests', '/contests'],
    ])
    expect(navigation.querySelectorAll('span[aria-hidden="true"]')).toHaveLength(2)
  })
})
