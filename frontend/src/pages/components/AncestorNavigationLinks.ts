export const breadcrumbLabelKeys = [
  'nav.dashboard',
  'siteManage.heading',
  'blog.heading',
  'blog.create.heading',
  'blog.detail.heading',
  'problem.list.heading',
  'problem.create.heading',
  'problem.detail.heading',
  'problem.submit.heading',
  'problem.data.heading',
  'problemSet.heading',
  'problemSet.create.heading',
  'problemSet.detail.heading',
  'submission.heading',
  'submission.detail.heading',
  'contest.heading',
  'contest.create.heading',
  'contest.detail.heading',
  'contest.registrants.heading',
  'contest.ranklist.heading',
  'contest.submissions.heading',
  'contest.manage.heading',
  'messages.heading',
  'messages.conversationTitle',
  'notifications.heading',
  'ranklist.heading',
  'ratingManage.heading',
  'userGroup.heading',
  'userGroup.create.heading',
  'userGroup.detail.heading',
  'userProfile.heading',
  'userSettings.heading',
] as const

export type BreadcrumbLabelKey = (typeof breadcrumbLabelKeys)[number]

export type BreadcrumbItem = {
  to: string
  labelKey: BreadcrumbLabelKey
  current: boolean
}

type BreadcrumbRouteNode = Omit<BreadcrumbItem, 'current'> & {
  parentTo: string | null
}

type BreadcrumbRouteMatcher = {
  pattern: RegExp
  build: (match: RegExpMatchArray, pathname: string) => BreadcrumbRouteNode
}

const hiddenBreadcrumbPaths = new Set(['/', '/login', '/register'])

const dashboardNode: BreadcrumbRouteNode = {
  to: '/',
  labelKey: 'nav.dashboard',
  parentTo: null,
}

function normalizePathname(pathname: string): string {
  if (pathname === '/') {
    return pathname
  }

  return pathname.replace(/\/+$/, '')
}

function routeNode(to: string, labelKey: BreadcrumbLabelKey, parentTo: string | null): BreadcrumbRouteNode {
  return { to, labelKey, parentTo }
}

function exactRoute(to: string, labelKey: BreadcrumbLabelKey, parentTo = '/'): BreadcrumbRouteMatcher {
  return {
    pattern: new RegExp(`^${to}$`),
    build: () => routeNode(to, labelKey, parentTo),
  }
}

function segment(match: RegExpMatchArray, index: number): string {
  const value = match[index]

  if (value === undefined) {
    throw new Error(`Breadcrumb route match is missing segment ${index}.`)
  }

  return value
}

const breadcrumbRouteMatchers: BreadcrumbRouteMatcher[] = [
  exactRoute('/site-manage', 'siteManage.heading'),
  exactRoute('/blogs', 'blog.heading'),
  exactRoute('/blogs/new', 'blog.create.heading', '/blogs'),
  {
    pattern: /^\/blogs\/[^/]+$/,
    build: (_, pathname) => routeNode(pathname, 'blog.detail.heading', '/blogs'),
  },
  {
    pattern: /^\/blog\/[^/]+$/,
    build: (_, pathname) => routeNode(pathname, 'blog.detail.heading', '/blogs'),
  },
  exactRoute('/problems', 'problem.list.heading'),
  exactRoute('/problems/new', 'problem.create.heading', '/problems'),
  {
    pattern: /^\/problems\/([^/]+)\/submit$/,
    build: (match, pathname) => routeNode(pathname, 'problem.submit.heading', `/problems/${segment(match, 1)}`),
  },
  {
    pattern: /^\/problems\/([^/]+)\/submissions$/,
    build: (match, pathname) => routeNode(pathname, 'submission.heading', `/problems/${segment(match, 1)}`),
  },
  {
    pattern: /^\/problems\/([^/]+)\/blogs$/,
    build: (match, pathname) => routeNode(pathname, 'blog.heading', `/problems/${segment(match, 1)}`),
  },
  {
    pattern: /^\/problems\/([^/]+)\/data$/,
    build: (match, pathname) => routeNode(pathname, 'problem.data.heading', `/problems/${segment(match, 1)}`),
  },
  {
    pattern: /^\/problems\/[^/]+$/,
    build: (_, pathname) => routeNode(pathname, 'problem.detail.heading', '/problems'),
  },
  exactRoute('/problem-sets', 'problemSet.heading'),
  exactRoute('/problem-sets/new', 'problemSet.create.heading', '/problem-sets'),
  {
    pattern: /^\/problem-sets\/[^/]+$/,
    build: (_, pathname) => routeNode(pathname, 'problemSet.detail.heading', '/problem-sets'),
  },
  exactRoute('/submissions', 'submission.heading'),
  {
    pattern: /^\/submissions\/[^/]+$/,
    build: (_, pathname) => routeNode(pathname, 'submission.detail.heading', '/submissions'),
  },
  exactRoute('/contests', 'contest.heading'),
  exactRoute('/contests/new', 'contest.create.heading', '/contests'),
  {
    pattern: /^\/contests\/([^/]+)\/problems\/([^/]+)\/submit$/,
    build: (match, pathname) =>
      routeNode(pathname, 'problem.submit.heading', `/contests/${segment(match, 1)}/problems/${segment(match, 2)}`),
  },
  {
    pattern: /^\/contests\/([^/]+)\/problems\/([^/]+)\/data$/,
    build: (match, pathname) =>
      routeNode(pathname, 'problem.data.heading', `/contests/${segment(match, 1)}/problems/${segment(match, 2)}`),
  },
  {
    pattern: /^\/contests\/([^/]+)\/problems\/[^/]+$/,
    build: (match, pathname) => routeNode(pathname, 'problem.detail.heading', `/contests/${segment(match, 1)}`),
  },
  {
    pattern: /^\/contests\/([^/]+)\/registrants$/,
    build: (match, pathname) => routeNode(pathname, 'contest.registrants.heading', `/contests/${segment(match, 1)}`),
  },
  {
    pattern: /^\/contests\/([^/]+)\/ranklist$/,
    build: (match, pathname) => routeNode(pathname, 'contest.ranklist.heading', `/contests/${segment(match, 1)}`),
  },
  {
    pattern: /^\/contests\/([^/]+)\/submissions$/,
    build: (match, pathname) => routeNode(pathname, 'contest.submissions.heading', `/contests/${segment(match, 1)}`),
  },
  {
    pattern: /^\/contests\/([^/]+)\/manage$/,
    build: (match, pathname) => routeNode(pathname, 'contest.manage.heading', `/contests/${segment(match, 1)}`),
  },
  {
    pattern: /^\/contests\/[^/]+$/,
    build: (_, pathname) => routeNode(pathname, 'contest.detail.heading', '/contests'),
  },
  exactRoute('/messages', 'messages.heading'),
  {
    pattern: /^\/messages\/with\/[^/]+$/,
    build: (_, pathname) => routeNode(pathname, 'messages.conversationTitle', '/messages'),
  },
  exactRoute('/notifications', 'notifications.heading'),
  exactRoute('/ranklist', 'ranklist.heading'),
  exactRoute('/ratings/manage', 'ratingManage.heading'),
  exactRoute('/user-groups', 'userGroup.heading'),
  exactRoute('/user-groups/new', 'userGroup.create.heading', '/user-groups'),
  {
    pattern: /^\/user-groups\/[^/]+$/,
    build: (_, pathname) => routeNode(pathname, 'userGroup.detail.heading', '/user-groups'),
  },
  {
    pattern: /^\/user\/([^/]+)\/blogs$/,
    build: (match, pathname) => routeNode(pathname, 'blog.heading', `/user/${segment(match, 1)}`),
  },
  {
    pattern: /^\/user\/([^/]+)\/settings$/,
    build: (match, pathname) => routeNode(pathname, 'userSettings.heading', `/user/${segment(match, 1)}`),
  },
  {
    pattern: /^\/user\/[^/]+$/,
    build: (_, pathname) => routeNode(pathname, 'userProfile.heading', '/'),
  },
]

function findBreadcrumbRouteNode(pathname: string): BreadcrumbRouteNode | null {
  if (pathname === '/') {
    return dashboardNode
  }

  const matcher = breadcrumbRouteMatchers.find((currentMatcher) => currentMatcher.pattern.test(pathname))

  if (!matcher) {
    return null
  }

  const match = pathname.match(matcher.pattern)

  if (!match) {
    return null
  }

  return matcher.build(match, pathname)
}

function toBreadcrumbItem(node: BreadcrumbRouteNode, current: boolean): BreadcrumbItem {
  return {
    to: node.to,
    labelKey: node.labelKey,
    current,
  }
}

export function buildBreadcrumbItems(pathname: string): BreadcrumbItem[] {
  const normalizedPathname = normalizePathname(pathname)

  if (hiddenBreadcrumbPaths.has(normalizedPathname)) {
    return []
  }

  const currentNode = findBreadcrumbRouteNode(normalizedPathname)

  if (!currentNode) {
    return [toBreadcrumbItem(dashboardNode, false)]
  }

  const nodes: BreadcrumbRouteNode[] = []
  const visitedPaths = new Set<string>()
  let node: BreadcrumbRouteNode | null = currentNode

  while (node && !visitedPaths.has(node.to)) {
    nodes.push(node)
    visitedPaths.add(node.to)
    node = node.parentTo === null ? null : findBreadcrumbRouteNode(node.parentTo)
  }

  return nodes.reverse().map((currentItem, index, items) => toBreadcrumbItem(currentItem, index === items.length - 1))
}
