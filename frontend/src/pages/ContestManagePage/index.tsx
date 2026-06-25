import { Navigate, useParams } from 'react-router-dom'

import { parseContestSlug } from '@/objects/contest/ContestSlug'
import { usePageTitle } from '@/pages/hooks/usePageTitle'
import { useSessionGuard } from '@/pages/hooks/useSessionGuard'
import { useI18n } from '@/system/i18n/use-i18n'

import { ContestManagePageContent } from './components/ContestManagePageContent'

/**
 * 比赛管理页入口，校验 slug 路由参数和管理员会话后渲染管理内容。
 */
export function ContestManagePage() {
  const { t } = useI18n()
  usePageTitle(t('contest.manage.pageTitle'))
  const { session: user, navigationIntent } = useSessionGuard()
  const { slug } = useParams<{ slug: string }>()

  if (navigationIntent) {
    return <Navigate replace={navigationIntent.replace} to={navigationIntent.to} />
  }

  if (!user) {
    return <Navigate replace to="/login" />
  }

  const slugResult = parseContestSlug(slug ?? '')
  if (!slugResult.ok) {
    return <Navigate replace to="/contests" />
  }

  return <ContestManagePageContent contestSlug={slugResult.value} />
}
