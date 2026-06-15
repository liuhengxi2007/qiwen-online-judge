import type { ReactNode } from 'react'

import { cn } from '@/components/ui/class-names'
import { useI18n } from '@/system/i18n/use-i18n'

import { AppSectionBar } from './AppSectionBar'
import { BreadcrumbNavigation } from './BreadcrumbNavigation'

/**
 * 标准页面外壳属性，定义标题、描述、右上操作区和布局 className 扩展点。
 */
type PageShellProps = {
  children: ReactNode
  title: ReactNode
  description?: ReactNode
  action?: ReactNode
  mainClassName?: string
  sectionClassName?: string
  siteNameClassName?: string
  titleClassName?: string
  headerClassName?: string
  showSectionBar?: boolean
}

/**
 * 标准页面外壳组件，统一顶部站点名、标题、祖先导航和主导航栏布局。
 */
export function PageShell({
  children,
  title,
  description,
  action,
  mainClassName,
  sectionClassName,
  siteNameClassName = 'text-slate-500',
  titleClassName = 'text-slate-950',
  headerClassName,
  showSectionBar = true,
}: PageShellProps) {
  const { t } = useI18n()

  return (
    <main className={cn('min-h-screen px-6 py-12 sm:px-8', mainClassName)}>
      <section className={cn('mx-auto max-w-6xl', sectionClassName)}>
        <div className={cn('mb-8 flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between', headerClassName)}>
          <div className="space-y-2">
            <p className={cn('text-sm uppercase tracking-[0.25em]', siteNameClassName)}>{t('common.siteName')}</p>
            <h1 className={cn('page-title-font text-4xl font-semibold tracking-tight', titleClassName)}>{title}</h1>
            {description ? <p className="text-sm text-slate-600">{description}</p> : null}
          </div>

          {action ?? <BreadcrumbNavigation />}
        </div>

        {showSectionBar ? <AppSectionBar /> : null}

        {children}
      </section>
    </main>
  )
}
