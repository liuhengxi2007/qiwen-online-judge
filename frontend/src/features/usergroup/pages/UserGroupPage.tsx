import { useEffect } from 'react'
import { Link, Navigate, useSearchParams } from 'react-router-dom'
import { ArrowRight, FolderKanban, Users } from 'lucide-react'

import { Alert, AlertDescription } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { useSessionGuard } from '@/features/auth/hooks/use-session-guard'
import { userGroupDescriptionValue, userGroupNameValue, userGroupSlugValue } from '@/features/usergroup/domain/usergroup'
import { useUserGroupPageModel } from '@/features/usergroup/hooks/use-usergroup-page-model'
import { AppSectionBar } from '@/shared/components/app-section-bar'
import { AncestorNavigation } from '@/shared/components/ancestor-navigation'
import { usePageTitle } from '@/shared/hooks/use-page-title'
import { useI18n } from '@/shared/i18n/i18n'
import { buildPageNumbers, calculateTotalPages, getPageCorrection, parsePositivePage } from '@/shared/domain/pagination'

const userGroupsPerPage = 10

export function UserGroupPage() {
  const { t } = useI18n()
  usePageTitle(t('userGroup.pageTitle'))
  const { session: user, navigationIntent } = useSessionGuard()
  const [searchParams, setSearchParams] = useSearchParams()
  const currentPage = parsePositivePage(searchParams.get('page'))
  const model = useUserGroupPageModel({ page: currentPage, pageSize: userGroupsPerPage })
  const totalPages = calculateTotalPages(model.totalItems, model.pageSize)
  const pageNumbers = buildPageNumbers(currentPage, totalPages)

  useEffect(() => {
    if (model.isLoading) {
      return
    }
    const correction = getPageCorrection(currentPage, totalPages)
    if (correction.kind === 'none') {
      return
    }
    const nextSearchParams = new URLSearchParams(searchParams)
    if (correction.kind === 'delete') {
      nextSearchParams.delete('page')
    } else {
      nextSearchParams.set('page', String(correction.page))
    }
    setSearchParams(nextSearchParams)
  }, [currentPage, model.isLoading, searchParams, setSearchParams, totalPages])

  if (navigationIntent) {
    return <Navigate replace={navigationIntent.replace} to={navigationIntent.to} />
  }

  if (!user) {
    return <Navigate replace to="/login" />
  }

  return (
    <main className="min-h-screen bg-[linear-gradient(180deg,#f8fafc_0%,#eef5f8_100%)] px-6 py-12 sm:px-8">
      <section className="mx-auto max-w-6xl">
        <div className="mb-8 flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
          <div className="space-y-2">
            <p className="text-sm uppercase tracking-[0.25em] text-slate-500">{t('common.siteName')}</p>
            <h1 className="font-['Georgia'] text-4xl font-semibold tracking-tight text-slate-950">{t('userGroup.heading')}</h1>
          </div>

          <AncestorNavigation />
        </div>

        <AppSectionBar />

        {model.errorMessage ? (
          <Alert variant="destructive" className="mb-6 rounded-2xl border-rose-200 bg-rose-50/95">
            <AlertDescription className="text-rose-700">{model.errorMessage}</AlertDescription>
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
              <Button asChild className="rounded-2xl bg-emerald-300 text-emerald-950 hover:bg-emerald-400">
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
              <div className="flex flex-wrap items-center justify-center gap-2 pt-4">
                <Button type="button" variant="outline" className="rounded-2xl border-slate-300 bg-white" disabled={currentPage === 1} onClick={() => {
                  const nextSearchParams = new URLSearchParams(searchParams)
                  nextSearchParams.set('page', String(Math.max(1, currentPage - 1)))
                  setSearchParams(nextSearchParams)
                }}>{t('common.pagination.previous')}</Button>
                {pageNumbers.map((page) => (
                  <Button key={page} type="button" variant={page === currentPage ? 'default' : 'outline'} className={page === currentPage ? 'rounded-2xl bg-slate-950 text-white' : 'rounded-2xl border-slate-300 bg-white'} onClick={() => {
                    const nextSearchParams = new URLSearchParams(searchParams)
                    nextSearchParams.set('page', String(page))
                    setSearchParams(nextSearchParams)
                  }}>{page}</Button>
                ))}
                <Button type="button" variant="outline" className="rounded-2xl border-slate-300 bg-white" disabled={currentPage === totalPages} onClick={() => {
                  const nextSearchParams = new URLSearchParams(searchParams)
                  nextSearchParams.set('page', String(Math.min(totalPages, currentPage + 1)))
                  setSearchParams(nextSearchParams)
                }}>{t('common.pagination.next')}</Button>
              </div>
            ) : null}
          </CardContent>
        </Card>
      </section>
    </main>
  )
}
