import { Link, useLocation } from 'react-router-dom'
import { ArrowLeft } from 'lucide-react'

import { Button } from '@/components/ui/button'

type AncestorLink = {
  to: string
  label: string
}

function buildAncestorLinks(pathname: string): AncestorLink[] {
  if (pathname === '/' || pathname === '/login' || pathname === '/register') {
    return []
  }

  if (pathname === '/site-manage') {
    return [{ to: '/', label: 'Dashboard' }]
  }

  if (pathname === '/problems') {
    return [{ to: '/', label: 'Dashboard' }]
  }

  if (pathname === '/problems/new') {
    return [
      { to: '/', label: 'Dashboard' },
      { to: '/problems', label: 'Problems' },
    ]
  }

  if (/^\/problems\/[^/]+$/.test(pathname)) {
    return [
      { to: '/', label: 'Dashboard' },
      { to: '/problems', label: 'Problems' },
    ]
  }

  if (/^\/problems\/[^/]+\/(submit|data)$/.test(pathname)) {
    const slug = pathname.split('/')[2]
    return [
      { to: '/', label: 'Dashboard' },
      { to: '/problems', label: 'Problems' },
      { to: `/problems/${slug}`, label: 'Problem' },
    ]
  }

  if (pathname === '/problem-sets') {
    return [{ to: '/', label: 'Dashboard' }]
  }

  if (pathname === '/problem-sets/new') {
    return [
      { to: '/', label: 'Dashboard' },
      { to: '/problem-sets', label: 'Problem Sets' },
    ]
  }

  if (/^\/problem-sets\/[^/]+$/.test(pathname)) {
    return [
      { to: '/', label: 'Dashboard' },
      { to: '/problem-sets', label: 'Problem Sets' },
    ]
  }

  if (pathname === '/submissions') {
    return [{ to: '/', label: 'Dashboard' }]
  }

  if (/^\/submissions\/[^/]+$/.test(pathname)) {
    return [
      { to: '/', label: 'Dashboard' },
      { to: '/submissions', label: 'Submissions' },
    ]
  }

  if (pathname === '/user-groups') {
    return [{ to: '/', label: 'Dashboard' }]
  }

  if (pathname === '/user-groups/new') {
    return [
      { to: '/', label: 'Dashboard' },
      { to: '/user-groups', label: 'User Groups' },
    ]
  }

  if (/^\/user-groups\/[^/]+$/.test(pathname)) {
    return [
      { to: '/', label: 'Dashboard' },
      { to: '/user-groups', label: 'User Groups' },
    ]
  }

  if (/^\/user\/[^/]+\/settings$/.test(pathname)) {
    return [{ to: '/', label: 'Dashboard' }]
  }

  return [{ to: '/', label: 'Dashboard' }]
}

export function AncestorNavigation({
  buttonClassName = 'rounded-full border-slate-300 bg-white',
}: {
  buttonClassName?: string
}) {
  const { pathname } = useLocation()
  const ancestorLinks = buildAncestorLinks(pathname)

  if (ancestorLinks.length === 0) {
    return null
  }

  return (
    <div className="flex flex-wrap gap-2">
      {ancestorLinks.map((link) => (
        <Button key={link.to} asChild variant="outline" className={buttonClassName}>
          <Link to={link.to}>
            <ArrowLeft className="size-4" />
            Back to {link.label}
          </Link>
        </Button>
      ))}
    </div>
  )
}
