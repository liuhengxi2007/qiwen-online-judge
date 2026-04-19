import { NavLink } from 'react-router-dom'

import { useAuthStore } from '@/features/auth/stores/use-auth-store'
import { AccountActions } from '@/shared/components/account-actions'
import { useI18n } from '@/shared/i18n/i18n'

type NavItem = {
  to: string
  label: string
  tone:
    | 'emerald'
    | 'rose'
    | 'indigo'
    | 'orange'
    | 'amber'
    | 'sky'
}

function itemClassName(tone: NavItem['tone'], isActive: boolean): string {
  const tones: Record<NavItem['tone'], { active: string; idle: string }> = {
    emerald: {
      active: 'bg-emerald-300 text-emerald-950 hover:bg-emerald-400',
      idle: 'text-emerald-800 hover:bg-emerald-50',
    },
    rose: {
      active: 'bg-rose-300 text-rose-950 hover:bg-rose-400',
      idle: 'text-rose-800 hover:bg-rose-50',
    },
    indigo: {
      active: 'bg-indigo-300 text-indigo-950 hover:bg-indigo-400',
      idle: 'text-indigo-800 hover:bg-indigo-50',
    },
    orange: {
      active: 'bg-orange-300 text-orange-950 hover:bg-orange-400',
      idle: 'text-orange-800 hover:bg-orange-50',
    },
    amber: {
      active: 'bg-amber-300 text-amber-950 hover:bg-amber-400',
      idle: 'text-amber-800 hover:bg-amber-50',
    },
    sky: {
      active: 'bg-sky-300 text-sky-950 hover:bg-sky-400',
      idle: 'text-sky-800 hover:bg-sky-50',
    },
  }

  return isActive
    ? `rounded-xl px-3 py-1.5 text-sm font-semibold transition ${tones[tone].active}`
    : `rounded-xl px-3 py-1.5 text-sm font-medium transition ${tones[tone].idle}`
}

export function AppSectionBar() {
  const { t } = useI18n()
  const session = useAuthStore((state) => state.session)

  if (!session) {
    return null
  }

  const items: NavItem[] = [
    { to: '/problems', label: t('nav.problems'), tone: 'emerald' },
    { to: '/problem-sets', label: t('nav.problemSets'), tone: 'rose' },
    { to: '/submissions', label: t('nav.submissions'), tone: 'indigo' },
    { to: '/blogs', label: t('nav.blogs'), tone: 'orange' },
    { to: '/ranklist', label: t('nav.ranklist'), tone: 'amber' },
    { to: '/user-groups', label: t('nav.userGroups'), tone: 'sky' },
  ]

  if (session.siteManager) {
    items.push({ to: '/site-manage', label: t('dashboard.siteManage.title'), tone: 'amber' })
  }

  return (
    <div className="mb-8 rounded-[1.5rem] border border-slate-200/80 bg-slate-100/90 px-3 py-3 shadow-[0_18px_45px_rgba(15,23,42,0.08)] backdrop-blur">
      <div className="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
        <nav className="flex flex-wrap items-center gap-1.5">
          {items.map((item, index) => (
            <div key={item.to} className="flex items-center gap-1.5">
              {index > 0 ? <span aria-hidden className="mx-0.5 h-5 w-px rounded-full bg-slate-300" /> : null}
              <NavLink className={({ isActive }) => itemClassName(item.tone, isActive)} to={item.to}>
                {item.label}
              </NavLink>
            </div>
          ))}
        </nav>
        <AccountActions />
      </div>
    </div>
  )
}
