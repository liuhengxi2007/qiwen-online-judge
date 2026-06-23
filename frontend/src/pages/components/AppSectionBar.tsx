import { NavLink } from 'react-router-dom'

import { useAuthStore } from '@/pages/stores/auth/UseAuthStore'
import { AccountActions } from '@/pages/components/AccountActions'
import { getNavAppEntries } from '@/pages/objects/AppEntryCatalog'
import { appModuleThemes, type AppModuleTheme } from '@/pages/objects/AppModuleTheme'
import { useI18n } from '@/system/i18n/use-i18n'

/**
 * 根据模块主题和激活状态生成 NavLink className。
 */
function itemClassName(theme: AppModuleTheme, isActive: boolean): string {
  return isActive
    ? `rounded-xl px-3 py-1.5 text-sm font-semibold transition ${theme.navActive}`
    : `rounded-xl px-3 py-1.5 text-sm font-medium transition ${theme.navIdle}`
}

/**
 * 登录后页面的主模块导航条，同时承载账号操作区和未读入口。
 */
export function AppSectionBar() {
  const { t } = useI18n()
  const session = useAuthStore((state) => state.session)

  if (!session) {
    return null
  }

  const items = getNavAppEntries()

  return (
    <div className="mb-8 rounded-[1.5rem] border border-slate-200/80 bg-slate-100/90 px-3 py-3 shadow-[0_18px_45px_rgba(15,23,42,0.08)] backdrop-blur">
      <div className="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
        <nav className="flex flex-wrap items-center gap-1.5">
          {items.map((item) => (
            <NavLink
              key={item.to}
              className={({ isActive }) => itemClassName(appModuleThemes[item.tone], isActive)}
              to={item.to}
            >
              {t(item.navLabelKey)}
            </NavLink>
          ))}
        </nav>
        <AccountActions />
      </div>
    </div>
  )
}
