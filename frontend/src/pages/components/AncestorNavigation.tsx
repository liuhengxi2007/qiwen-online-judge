import { Link, useLocation } from 'react-router-dom'
import { ArrowLeft } from 'lucide-react'

import { Button } from '@/components/ui/button'
import { useI18n } from '@/system/i18n/use-i18n'
import { buildAncestorLinks } from './AncestorNavigationLinks'

/**
 * 当前页面的返回导航组件，根据 pathname 自动渲染可返回的上级入口。
 */
export function AncestorNavigation({
  buttonClassName = 'rounded-full border-slate-300 bg-white',
}: {
  buttonClassName?: string
}) {
  const { pathname } = useLocation()
  const { t } = useI18n()
  const ancestorLinks = buildAncestorLinks(pathname)

  return (
    <div className="flex flex-wrap gap-2">
      {ancestorLinks.map((link) => (
        <Button key={link.to} asChild variant="outline" className={buttonClassName}>
          <Link to={link.to}>
            <ArrowLeft className="size-4" />
            {t('nav.backTo', { label: t(`nav.${link.labelKey}`) })}
          </Link>
        </Button>
      ))}
    </div>
  )
}
