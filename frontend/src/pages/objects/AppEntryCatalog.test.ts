import { describe, expect, it } from 'vitest'

import { enMessages } from '@/system/i18n/messages/en'
import { zhCnMessages } from '@/system/i18n/messages/zh-CN'
import { appDashboardGroups, appEntries, getDashboardEntryGroups, getNavAppEntries } from './AppEntryCatalog'

describe('app-entry-catalog', () => {
  it('keeps the public nav to the first six dashboard entries', () => {
    expect(getNavAppEntries().map((entry) => entry.to)).toEqual([
      '/problems',
      '/submissions',
      '/hacks',
      '/problem-sets',
      '/blogs',
      '/contests',
    ])
  })

  it('keeps site manager entries out of the public nav', () => {
    expect(getNavAppEntries().every((entry) => entry.requiresSiteManager !== true)).toBe(true)
  })

  it('groups dashboard entries by functional domain for regular users', () => {
    expect(getDashboardEntryGroups(false).map((group) => [group.id, group.entries.map((entry) => entry.id)])).toEqual([
      ['problemCore', ['problems', 'submissions', 'hacks']],
      ['contentAndContest', ['problemSets', 'blogs', 'contests']],
      ['userAndRank', ['userGroups', 'ranklist']],
    ])
  })

  it('adds the admin group for site managers', () => {
    expect(getDashboardEntryGroups(true).map((group) => [group.id, group.entries.map((entry) => entry.id)])).toEqual([
      ['problemCore', ['problems', 'submissions', 'hacks']],
      ['contentAndContest', ['problemSets', 'blogs', 'contests']],
      ['userAndRank', ['userGroups', 'ranklist']],
      ['admin', ['ratingManage', 'siteManage']],
    ])
  })

  it('keeps every configured entry label present in English and Chinese', () => {
    const labelKeys = [
      ...appDashboardGroups.map((group) => group.titleKey),
      ...appEntries.flatMap((entry) => [
        entry.navLabelKey,
        entry.dashboardTitleKey,
        entry.dashboardDescriptionKey,
        entry.dashboardOpenKey,
      ]),
    ]

    for (const labelKey of labelKeys) {
      expect(enMessages[labelKey], labelKey).toBeDefined()
      expect(zhCnMessages[labelKey], labelKey).toBeDefined()
    }
  })
})
