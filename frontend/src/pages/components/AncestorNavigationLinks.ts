export type AncestorLink = {
  to: string
  labelKey:
    | 'dashboard'
    | 'blogs'
    | 'notifications'
    | 'problems'
    | 'problem'
    | 'problemSets'
    | 'messages'
    | 'submissions'
    | 'ranklist'
    | 'userGroups'
    | 'contests'
}

function buildAncestorLinksForPath(pathname: string): AncestorLink[] {
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

  if (pathname === '/contests') {
    return [{ to: '/', labelKey: 'dashboard' }]
  }

  if (pathname === '/contests/new') {
    return [
      { to: '/', labelKey: 'dashboard' },
      { to: '/contests', labelKey: 'contests' },
    ]
  }

  if (/^\/contests\/[^/]+$/.test(pathname)) {
    return [
      { to: '/', labelKey: 'dashboard' },
      { to: '/contests', labelKey: 'contests' },
    ]
  }

  const contestProblemMatch =
    pathname.match(/^\/contests\/([^/]+)\/problems\/([^/]+)(?:\/(submit|data))?$/)

  if (contestProblemMatch) {
    const contestSlug = contestProblemMatch[1]
    const problemSlug = contestProblemMatch[2]

    return [
      { to: '/', labelKey: 'dashboard' },
      { to: '/contests', labelKey: 'contests' },
      { to: `/contests/${contestSlug}`, labelKey: 'contests' },
      { to: `/contests/${contestSlug}/problems/${problemSlug}`, labelKey: 'problem' },
    ]
  }

  if (/^\/contests\/[^/]+\/ranklist$/.test(pathname)) {
    const slug = pathname.split('/')[2]
    return [
      { to: '/', labelKey: 'dashboard' },
      { to: '/contests', labelKey: 'contests' },
      { to: `/contests/${slug}`, labelKey: 'contests' },
      { to: `${pathname}`, labelKey: 'ranklist' },
    ]
  }

  if (/^\/contests\/[^/]+\/submissions$/.test(pathname)) {
    const slug = pathname.split('/')[2]
    return [
      { to: '/', labelKey: 'dashboard' },
      { to: '/contests', labelKey: 'contests' },
      { to: `/contests/${slug}`, labelKey: 'contests' },
      { to: `${pathname}`, labelKey: 'submissions' },
    ]
  }

  if (/^\/contests\/[^/]+\/manage$/.test(pathname)) {
    const slug = pathname.split('/')[2]
    return [
      { to: '/', labelKey: 'dashboard' },
      { to: '/contests', labelKey: 'contests' },
      { to: `/contests/${slug}`, labelKey: 'contests' },
    ]
  }

  if (/^\/contests\/[^/]+\/registrants$/.test(pathname)) {
    const slug = pathname.split('/')[2]
    return [
      { to: '/', labelKey: 'dashboard' },
      { to: '/contests', labelKey: 'contests' },
      { to: `/contests/${slug}`, labelKey: 'contests' },
    ]
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

export function buildAncestorLinks(pathname: string): AncestorLink[] {
  return buildAncestorLinksForPath(pathname).filter((link) => link.to !== pathname)
}
