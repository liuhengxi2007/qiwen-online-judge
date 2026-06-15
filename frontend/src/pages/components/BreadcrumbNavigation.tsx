import { Link, useLocation } from 'react-router-dom'
import { ChevronRight } from 'lucide-react'

import { cn } from '@/components/ui/class-names'
import { useI18n } from '@/system/i18n/use-i18n'
import { buildBreadcrumbItems } from './AncestorNavigationLinks'

export function BreadcrumbNavigation() {
  const { pathname } = useLocation()
  const { t } = useI18n()
  const breadcrumbItems = buildBreadcrumbItems(pathname)

  if (breadcrumbItems.length === 0) {
    return null
  }

  return (
    <nav aria-label="Breadcrumb" className="min-w-0 text-sm text-slate-500">
      <ol className="flex flex-wrap items-center gap-x-1 gap-y-2 sm:justify-end">
        {breadcrumbItems.map((item, index) => (
          <li key={`${item.to}-${index}`} className="flex min-w-0 items-center gap-1">
            {index > 0 ? <ChevronRight aria-hidden="true" className="size-4 shrink-0 text-slate-400" /> : null}
            {item.current ? (
              <span aria-current="page" className="max-w-48 truncate font-medium text-slate-900">
                {t(item.labelKey)}
              </span>
            ) : (
              <Link
                className={cn(
                  'max-w-48 truncate rounded-sm font-medium text-slate-500 outline-none transition-colors',
                  'hover:text-slate-950 focus-visible:ring-2 focus-visible:ring-slate-400 focus-visible:ring-offset-2',
                )}
                to={item.to}
              >
                {t(item.labelKey)}
              </Link>
            )}
          </li>
        ))}
      </ol>
    </nav>
  )
}
