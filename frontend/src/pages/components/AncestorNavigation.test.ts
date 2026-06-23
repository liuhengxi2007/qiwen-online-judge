import { createElement } from 'react'
import { MemoryRouter } from 'react-router-dom'
import { cleanup, render, screen } from '@testing-library/react'
import { afterEach, describe, expect, it, vi } from 'vitest'

import { I18nContext } from '@/system/i18n/i18n-context'
import { enMessages } from '@/system/i18n/messages/en'
import { zhCnMessages } from '@/system/i18n/messages/zh-CN'
import { BreadcrumbNavigation } from './BreadcrumbNavigation'
import { breadcrumbLabelKeys, buildBreadcrumbItems, type BreadcrumbItem } from './AncestorNavigationLinks'

const dashboard: BreadcrumbItem = { to: '/', labelKey: 'nav.dashboard', current: false }

function item(to: string, labelKey: BreadcrumbItem['labelKey'], current = false): BreadcrumbItem {
  return { to, labelKey, current }
}

function renderBreadcrumb(pathname: string) {
  return render(
    createElement(
      I18nContext.Provider,
      {
        value: {
          locale: 'en',
          setLocale: vi.fn(),
          t: (key: string) => key,
        },
      },
      createElement(MemoryRouter, { initialEntries: [pathname] }, createElement(BreadcrumbNavigation)),
    ),
  )
}

afterEach(() => {
  cleanup()
})

describe('buildBreadcrumbItems route chains', () => {
  it('hides breadcrumbs on root and auth pages', () => {
    expect(buildBreadcrumbItems('/')).toEqual([])
    expect(buildBreadcrumbItems('/login')).toEqual([])
    expect(buildBreadcrumbItems('/register')).toEqual([])
  })

  it('includes dashboard and the current list page', () => {
    expect(buildBreadcrumbItems('/contests')).toEqual([dashboard, item('/contests', 'contest.heading', true)])
  })

  it('maps contest detail with explicit list and detail labels', () => {
    expect(buildBreadcrumbItems('/contests/sample')).toEqual([
      dashboard,
      item('/contests', 'contest.heading'),
      item('/contests/sample', 'contest.detail.heading', true),
    ])
  })

  it('maps create pages under their owning list pages', () => {
    expect(buildBreadcrumbItems('/problems/new')).toEqual([
      dashboard,
      item('/problems', 'problem.list.heading'),
      item('/problems/new', 'problem.create.heading', true),
    ])
  })

  it('maps contest problem data through contest and problem detail', () => {
    expect(buildBreadcrumbItems('/contests/sample/problems/a/data')).toEqual([
      dashboard,
      item('/contests', 'contest.heading'),
      item('/contests/sample', 'contest.detail.heading'),
      item('/contests/sample/problems/a', 'problem.detail.heading'),
      item('/contests/sample/problems/a/data', 'problem.data.heading', true),
    ])
  })

  it('maps problem blogs under problem detail', () => {
    expect(buildBreadcrumbItems('/problems/two-sum/blogs')).toEqual([
      dashboard,
      item('/problems', 'problem.list.heading'),
      item('/problems/two-sum', 'problem.detail.heading'),
      item('/problems/two-sum/blogs', 'blog.heading', true),
    ])
  })

  it('maps message conversations under the message list', () => {
    expect(buildBreadcrumbItems('/messages/with/alice')).toEqual([
      dashboard,
      item('/messages', 'messages.heading'),
      item('/messages/with/alice', 'messages.conversationTitle', true),
    ])
  })

  it('maps hack pages under their owning lists', () => {
    expect(buildBreadcrumbItems('/hacks/123')).toEqual([
      dashboard,
      item('/hacks', 'hack.list.heading'),
      item('/hacks/123', 'hack.detail.heading', true),
    ])
    expect(buildBreadcrumbItems('/submissions/42/hack/1')).toEqual([
      dashboard,
      item('/submissions', 'submission.heading'),
      item('/submissions/42', 'submission.detail.heading'),
      item('/submissions/42/hack/1', 'hack.submit.heading', true),
    ])
  })

  it('maps user blogs and settings under user detail', () => {
    expect(buildBreadcrumbItems('/user/alice/blogs')).toEqual([
      dashboard,
      item('/user/alice', 'userProfile.heading'),
      item('/user/alice/blogs', 'blog.heading', true),
    ])
    expect(buildBreadcrumbItems('/user/alice/settings')).toEqual([
      dashboard,
      item('/user/alice', 'userProfile.heading'),
      item('/user/alice/settings', 'userSettings.heading', true),
    ])
  })

  it('falls back to a dashboard link for unknown paths', () => {
    expect(buildBreadcrumbItems('/unknown/path')).toEqual([dashboard])
  })
})

describe('BreadcrumbNavigation', () => {
  it('renders the current page without a self link', () => {
    renderBreadcrumb('/contests/sample')

    expect(screen.getByRole('navigation', { name: 'Breadcrumb' })).toBeTruthy()
    expect(screen.getByRole('link', { name: 'nav.dashboard' }).getAttribute('href')).toBe('/')
    expect(screen.getByRole('link', { name: 'contest.heading' }).getAttribute('href')).toBe('/contests')
    expect(screen.getByText('contest.detail.heading').getAttribute('aria-current')).toBe('page')
    expect(screen.queryByRole('link', { name: 'contest.detail.heading' })).toBeNull()
  })
})

describe('breadcrumb i18n keys', () => {
  it('keeps every breadcrumb label key present in English and Chinese', () => {
    for (const labelKey of breadcrumbLabelKeys) {
      expect(enMessages[labelKey], labelKey).toBeDefined()
      expect(zhCnMessages[labelKey], labelKey).toBeDefined()
    }
  })
})
