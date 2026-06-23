import type { LucideIcon } from 'lucide-react'
import { BookCopy, CalendarDays, FileText, Files, Gauge, NotebookPen, Target, Trophy, Users, UsersRound } from 'lucide-react'

import type { AppModuleTone } from '@/pages/objects/AppModuleTheme'

/**
 * 仪表盘入口分组 ID，按功能域组织所有业务入口。
 */
export type AppDashboardGroupId = 'problem' | 'judge' | 'contest' | 'community' | 'admin'

/**
 * 应用入口定义，统一驱动顶部导航和仪表盘卡片。
 */
export type AppEntry = {
  id: string
  to: string
  navLabelKey: string
  dashboardTitleKey: string
  dashboardDescriptionKey: string
  dashboardOpenKey: string
  icon: LucideIcon
  tone: AppModuleTone
  dashboardGroup: AppDashboardGroupId
  showInNav: boolean
  requiresSiteManager?: boolean
}

/**
 * 仪表盘分组定义，只包含分组标题和排序。
 */
export type AppDashboardGroup = {
  id: AppDashboardGroupId
  titleKey: string
}

/**
 * 带入口的仪表盘分组，用于渲染时跳过空分组。
 */
export type AppDashboardEntryGroup = AppDashboardGroup & {
  entries: AppEntry[]
}

export const appDashboardGroups: AppDashboardGroup[] = [
  { id: 'problem', titleKey: 'dashboard.group.problem' },
  { id: 'judge', titleKey: 'dashboard.group.judge' },
  { id: 'contest', titleKey: 'dashboard.group.contest' },
  { id: 'community', titleKey: 'dashboard.group.community' },
  { id: 'admin', titleKey: 'dashboard.group.admin' },
]

export const appEntries: AppEntry[] = [
  {
    id: 'problems',
    to: '/problems',
    navLabelKey: 'nav.problems',
    dashboardTitleKey: 'dashboard.problems.title',
    dashboardDescriptionKey: 'dashboard.problems.description',
    dashboardOpenKey: 'dashboard.problems.open',
    icon: FileText,
    tone: 'emerald',
    dashboardGroup: 'problem',
    showInNav: true,
  },
  {
    id: 'problemSets',
    to: '/problem-sets',
    navLabelKey: 'nav.problemSets',
    dashboardTitleKey: 'dashboard.problemSets.title',
    dashboardDescriptionKey: 'dashboard.problemSets.description',
    dashboardOpenKey: 'dashboard.problemSets.open',
    icon: BookCopy,
    tone: 'rose',
    dashboardGroup: 'problem',
    showInNav: true,
  },
  {
    id: 'submissions',
    to: '/submissions',
    navLabelKey: 'nav.submissions',
    dashboardTitleKey: 'dashboard.submissions.title',
    dashboardDescriptionKey: 'dashboard.submissions.description',
    dashboardOpenKey: 'dashboard.submissions.open',
    icon: Files,
    tone: 'indigo',
    dashboardGroup: 'judge',
    showInNav: true,
  },
  {
    id: 'hacks',
    to: '/hacks',
    navLabelKey: 'nav.hacks',
    dashboardTitleKey: 'dashboard.hacks.title',
    dashboardDescriptionKey: 'dashboard.hacks.description',
    dashboardOpenKey: 'dashboard.hacks.open',
    icon: Target,
    tone: 'red',
    dashboardGroup: 'judge',
    showInNav: true,
  },
  {
    id: 'contests',
    to: '/contests',
    navLabelKey: 'nav.contests',
    dashboardTitleKey: 'dashboard.contests.title',
    dashboardDescriptionKey: 'dashboard.contests.description',
    dashboardOpenKey: 'dashboard.contests.open',
    icon: CalendarDays,
    tone: 'cyan',
    dashboardGroup: 'contest',
    showInNav: true,
  },
  {
    id: 'ranklist',
    to: '/ranklist',
    navLabelKey: 'nav.ranklist',
    dashboardTitleKey: 'dashboard.ranklist.title',
    dashboardDescriptionKey: 'dashboard.ranklist.description',
    dashboardOpenKey: 'dashboard.ranklist.open',
    icon: Trophy,
    tone: 'amber',
    dashboardGroup: 'contest',
    showInNav: true,
  },
  {
    id: 'blogs',
    to: '/blogs',
    navLabelKey: 'nav.blogs',
    dashboardTitleKey: 'dashboard.blogs.title',
    dashboardDescriptionKey: 'dashboard.blogs.description',
    dashboardOpenKey: 'dashboard.blogs.open',
    icon: NotebookPen,
    tone: 'orange',
    dashboardGroup: 'community',
    showInNav: true,
  },
  {
    id: 'userGroups',
    to: '/user-groups',
    navLabelKey: 'nav.userGroups',
    dashboardTitleKey: 'dashboard.userGroups.title',
    dashboardDescriptionKey: 'dashboard.userGroups.description',
    dashboardOpenKey: 'dashboard.userGroups.open',
    icon: UsersRound,
    tone: 'sky',
    dashboardGroup: 'community',
    showInNav: true,
  },
  {
    id: 'ratingManage',
    to: '/ratings/manage',
    navLabelKey: 'ratingManage.heading',
    dashboardTitleKey: 'dashboard.ratingManage.title',
    dashboardDescriptionKey: 'dashboard.ratingManage.description',
    dashboardOpenKey: 'dashboard.ratingManage.open',
    icon: Gauge,
    tone: 'amber',
    dashboardGroup: 'admin',
    showInNav: false,
    requiresSiteManager: true,
  },
  {
    id: 'siteManage',
    to: '/site-manage',
    navLabelKey: 'siteManage.heading',
    dashboardTitleKey: 'dashboard.siteManage.title',
    dashboardDescriptionKey: 'dashboard.siteManage.description',
    dashboardOpenKey: 'dashboard.siteManage.open',
    icon: Users,
    tone: 'fuchsia',
    dashboardGroup: 'admin',
    showInNav: false,
    requiresSiteManager: true,
  },
]

export function getNavAppEntries(): AppEntry[] {
  return appEntries.filter((entry) => entry.showInNav)
}

export function getDashboardEntryGroups(showSiteManage: boolean): AppDashboardEntryGroup[] {
  return appDashboardGroups
    .map((group) => ({
      ...group,
      entries: appEntries.filter(
        (entry) => entry.dashboardGroup === group.id && (!entry.requiresSiteManager || showSiteManage),
      ),
    }))
    .filter((group) => group.entries.length > 0)
}
