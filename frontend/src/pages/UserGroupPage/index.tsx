import { Link, Navigate, useSearchParams } from 'react-router-dom'
import { ArrowRight, FolderKanban, Users } from 'lucide-react'

import { Alert, AlertDescription } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { useSessionGuard } from '@/pages/hooks/useSessionGuard'
import { userGroupDescriptionValue } from '@/objects/usergroup/UserGroupDescription'
import { userGroupNameValue } from '@/objects/usergroup/UserGroupName'
import { userGroupSlugValue } from '@/objects/usergroup/UserGroupSlug'
import { useUserGroupPageModel } from './hooks/useUserGroupPageModel'
import { PageShell } from '@/pages/components/PageShell'
import { PaginationControls } from '@/pages/components/PaginationControls'
import { usePageTitle } from '@/pages/hooks/usePageTitle'
import { useI18n } from '@/system/i18n/use-i18n'
import { calculateTotalPages, parsePositivePage } from '@/pages/objects/Pagination'
import { usePageSearchParamCorrection } from '@/pages/hooks/usePageSearchParamCorrection'

/**
 * 用户组列表默认每页数量。
 */
const userGroupsPerPage = 10

/**
 * 用户组列表页，负责会话守卫、分页查询、创建入口和列表展示。
 */
export function UserGroupPage() {
  const { t } = useI18n()
  usePageTitle(t('userGroup.pageTitle'))
  const { session: user, navigationIntent } = useSessionGuard()
  const [searchParams, setSearchParams] = useSearchParams()
  const currentPage = parsePositivePage(searchParams.get('page'))
  const model = useUserGroupPageModel({ page: currentPage, pageSize: userGroupsPerPage })
  const totalPages = calculateTotalPages(model.totalItems, model.pageSize)

  usePageSearchParamCorrection({
    currentPage,
    totalPages,
    isLoading: model.isLoading,
    setSearchParams,
  })

  if (navigationIntent) {
    return <Navigate replace={navigationIntent.replace} to={navigationIntent.to} />
  }

  if (!user) {
    return <Navigate replace to="/login" />
  }

  const onPageChange = (page: number) => {
    const nextSearchParams = new URLSearchParams(searchParams)
    nextSearchParams.set('page', String(page))
    setSearchParams(nextSearchParams)
  }

  return (
    <PageShell title={t('userGroup.heading')} mainClassName="bg-[linear-gradient(180deg,#f8fafc_0%,#eef5f8_100%)]">
      {model.errorMessage ? (
        <Alert variant="destructive" className="mb-6">
          <AlertDescription>{model.errorMessage}</AlertDescription>
        </Alert>
      ) : null}

      <Card className="border-slate-200 bg-white shadow-[0_24px_60px_rgba(15,23,42,0.08)]">
        <CardHeader>
          <div className="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
            <div className="flex items-center gap-3">
              <div className="flex size-12 items-center justify-center rounded-2xl bg-sky-100 text-sky-700">
                <FolderKanban className="size-5" />
              </div>
              <div>
                <CardTitle className="text-xl text-slate-950">{t('userGroup.list.cardTitle')}</CardTitle>
                <CardDescription>{t('userGroup.list.cardDescription')}</CardDescription>
              </div>
            </div>
            <Button asChild variant="create">
              <Link to="/user-groups/new">
                <Users className="size-4" />
                {t('userGroup.list.create')}
              </Link>
            </Button>
          </div>
        </CardHeader>
        <CardContent className="space-y-4">
          {model.isLoading ? (
            <p className="text-sm text-slate-500">{t('userGroup.list.loading')}</p>
          ) : model.groups.length === 0 ? (
            <p className="text-sm text-slate-500">{t('userGroup.list.empty')}</p>
          ) : (
            model.groups.map((group) => (
              <div key={group.id} className="rounded-2xl border border-slate-200 bg-slate-50 p-5">
                <div className="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
                  <div className="space-y-2">
                    <h2 className="text-lg font-semibold text-slate-950">{userGroupNameValue(group.name)}</h2>
                    <p className="font-mono text-xs text-slate-500">{userGroupSlugValue(group.slug)}</p>
                    <p className="text-sm leading-7 text-slate-600">
                      {userGroupDescriptionValue(group.description) || t('common.noDescription')}
                    </p>
                  </div>

                  <Button asChild variant="outline" className="rounded-2xl border-slate-300 bg-white">
                    <Link to={`/user-groups/${userGroupSlugValue(group.slug)}`}>
                      {t('userGroup.list.open')}
                      <ArrowRight className="size-4" />
                    </Link>
                  </Button>
                </div>
              </div>
            ))
          )}
          {!model.isLoading && model.groups.length > 0 && totalPages > 1 ? (
            <PaginationControls
              currentPage={currentPage}
              totalPages={totalPages}
              previousLabel={t('common.pagination.previous')}
              nextLabel={t('common.pagination.next')}
              onPageChange={onPageChange}
            />
          ) : null}
        </CardContent>
      </Card>
    </PageShell>
  )
}
