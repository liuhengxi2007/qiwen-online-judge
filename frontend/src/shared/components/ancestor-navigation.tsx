import { Link, useLocation } from 'react-router-dom'
import { ArrowLeft } from 'lucide-react'

import { Button } from '@/components/ui/button'
import { useI18n } from '@/shared/i18n/i18n'

type AncestorLink = {
  to: string
  labelKey: 'dashboard' | 'blogs' | 'notifications' | 'problems' | 'problem' | 'problemSets' | 'messages' | 'submissions' | 'userGroups'
}

function buildAncestorLinks(pathname: string): AncestorLink[] {
  if (pathname === '/' || pathname === '/login' || pathname === '/register') {
    return []
  }

  if (pathname === '/site-manage') {
    return [{ to: '/', labelKey: 'dashboard' }]
  }

  if (pathname === '/blogs') {
    return [{ to: '/', labelKey: 'dashboard' }]
  }

  if (pathname === '/blogs/new') {
    return [
      { to: '/', labelKey: 'dashboard' },
      { to: '/blogs', labelKey: 'blogs' },
    ]
  }

  if (/^\/blogs\/[^/]+$/.test(pathname)) {
    return [
      { to: '/', labelKey: 'dashboard' },
      { to: '/blogs', labelKey: 'blogs' },
    ]
  }

  if (/^\/blog\/[^/]+$/.test(pathname)) {
    return [
      { to: '/', labelKey: 'dashboard' },
      { to: '/blogs', labelKey: 'blogs' },
    ]
  }

  if (pathname === '/problems') {
    return [{ to: '/', labelKey: 'dashboard' }]
  }

  if (pathname === '/problems/new') {
    return [
      { to: '/', labelKey: 'dashboard' },
      { to: '/problems', labelKey: 'problems' },
    ]
  }

  if (/^\/problems\/[^/]+$/.test(pathname)) {
    return [
      { to: '/', labelKey: 'dashboard' },
      { to: '/problems', labelKey: 'problems' },
    ]
  }

  if (/^\/problems\/[^/]+\/(submit|data|submissions)$/.test(pathname)) {
    const slug = pathname.split('/')[2]
    return [
      { to: '/', labelKey: 'dashboard' },
      { to: '/problems', labelKey: 'problems' },
      { to: `/problems/${slug}`, labelKey: 'problem' },
    ]
  }

  if (pathname === '/problem-sets') {
    return [{ to: '/', labelKey: 'dashboard' }]
  }

  if (pathname === '/problem-sets/new') {
    return [
      { to: '/', labelKey: 'dashboard' },
      { to: '/problem-sets', labelKey: 'problemSets' },
    ]
  }

  if (/^\/problem-sets\/[^/]+$/.test(pathname)) {
    return [
      { to: '/', labelKey: 'dashboard' },
      { to: '/problem-sets', labelKey: 'problemSets' },
    ]
  }

  if (pathname === '/submissions') {
    return [{ to: '/', labelKey: 'dashboard' }]
  }

  if (pathname === '/messages') {
    return [{ to: '/', labelKey: 'dashboard' }]
  }

  if (pathname === '/notifications') {
    return [{ to: '/', labelKey: 'dashboard' }]
  }

  if (/^\/messages\/[^/]+$/.test(pathname)) {
    return [
      { to: '/', labelKey: 'dashboard' },
      { to: '/messages', labelKey: 'messages' },
    ]
  }

  if (/^\/submission\/[^/]+$/.test(pathname)) {
    return [
      { to: '/', labelKey: 'dashboard' },
      { to: '/submissions', labelKey: 'submissions' },
    ]
  }

  if (/^\/submissions\/[^/]+$/.test(pathname)) {
    return [
      { to: '/', labelKey: 'dashboard' },
      { to: '/submissions', labelKey: 'submissions' },
    ]
  }

  if (pathname === '/user-groups') {
    return [{ to: '/', labelKey: 'dashboard' }]
  }

  if (pathname === '/user-groups/new') {
    return [
      { to: '/', labelKey: 'dashboard' },
      { to: '/user-groups', labelKey: 'userGroups' },
    ]
  }

  if (/^\/user-groups\/[^/]+$/.test(pathname)) {
    return [
      { to: '/', labelKey: 'dashboard' },
      { to: '/user-groups', labelKey: 'userGroups' },
    ]
  }

  if (/^\/user\/[^/]+(\/settings)?$/.test(pathname)) {
    return [{ to: '/', labelKey: 'dashboard' }]
  }

  return [{ to: '/', labelKey: 'dashboard' }]
}

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
