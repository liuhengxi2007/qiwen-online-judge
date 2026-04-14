import { Link, Navigate } from 'react-router-dom'
import { Cpu, Settings2, Trash2 } from 'lucide-react'

import { Alert, AlertDescription } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Checkbox } from '@/components/ui/checkbox'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import {
  displayNameValue,
  emailAddressValue,
  usernameValue,
  type AuthUserListItem,
} from '@/features/auth/domain/auth'
import { AncestorNavigation } from '@/shared/components/ancestor-navigation'
import { ConfirmActionDialog } from '@/shared/components/confirm-action-dialog'
import { usePageTitle } from '@/shared/hooks/use-page-title'
import { useSiteManageModel } from '@/features/site-management/hooks/use-site-manage-model'
import { useSessionGuard } from '@/features/auth/hooks/use-session-guard'
import { useI18n } from '@/shared/i18n/i18n'

export function SiteManagePage() {
  const { t } = useI18n()
  usePageTitle(t('siteManage.pageTitle'))
  const { session: user, siteManagerSession, navigationIntent: guardNavigationIntent } =
    useSessionGuard({ requireSiteManager: true })
  const {
    users,
    judgers,
    userListError,
    judgerListError,
    statusMessage,
    isLoadingUsers,
    isLoadingJudgers,
    updatingUsername,
    deletingUsername,
    navigationIntent: modelNavigationIntent,
    savePermissions,
    deleteUser,
  } = useSiteManageModel(Boolean(siteManagerSession))

  if (guardNavigationIntent) {
    return <Navigate replace={guardNavigationIntent.replace} to={guardNavigationIntent.to} />
  }

  if (modelNavigationIntent) {
    return <Navigate replace={modelNavigationIntent.replace} to={modelNavigationIntent.to} />
  }

  if (!user) {
    return <Navigate replace to="/login" />
  }

  const isProtectedAdmin = (listedUser: AuthUserListItem) => usernameValue(listedUser.username) === 'admin'
  return (
    <main className="min-h-screen bg-[linear-gradient(180deg,#fffaf4_0%,#f4efe5_100%)] px-6 py-12 sm:px-8">
      <section className="mx-auto max-w-6xl">
        <div className="mb-8 flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
          <div className="space-y-2">
            <p className="text-sm uppercase tracking-[0.25em] text-stone-500">{t('common.siteName')}</p>
            <h1 className="font-['Georgia'] text-4xl font-semibold tracking-tight text-stone-950">
              {t('siteManage.heading')}
            </h1>
            <p className="text-sm text-stone-600">
              {t('common.signedInAs', { displayName: displayNameValue(user.displayName), username: usernameValue(user.username) })}
            </p>
          </div>

          <AncestorNavigation buttonClassName="rounded-full border-stone-300 bg-white" />
        </div>

        <Card className="border-stone-200 bg-white shadow-[0_24px_60px_rgba(28,25,23,0.08)]">
          <CardHeader>
            <div className="flex items-center gap-3">
              <div className="flex size-12 items-center justify-center rounded-2xl bg-amber-100 text-amber-700">
                <Settings2 className="size-5" />
              </div>
              <div>
                <CardTitle className="text-xl text-stone-950">{t('siteManage.userManagementTitle')}</CardTitle>
                <CardDescription>{t('siteManage.userManagementDescription')}</CardDescription>
              </div>
            </div>
          </CardHeader>
          <CardContent className="space-y-4">
            {statusMessage ? (
              <Alert className="rounded-2xl border-emerald-200 bg-emerald-50/95">
                <AlertDescription className="text-emerald-700">{statusMessage}</AlertDescription>
              </Alert>
            ) : null}
            {userListError ? (
              <Alert variant="destructive" className="rounded-2xl border-rose-200 bg-rose-50/95">
                <AlertDescription className="text-rose-700">{userListError}</AlertDescription>
              </Alert>
            ) : null}
            {isLoadingUsers ? (
              <p className="text-sm text-stone-500">{t('siteManage.loadingUsers')}</p>
            ) : users.length === 0 ? (
              <div className="rounded-3xl border border-dashed border-stone-300 bg-stone-50 px-6 py-10 text-center">
                <p className="text-base font-medium text-stone-900">{t('siteManage.emptyUsersTitle')}</p>
                <p className="mt-2 text-sm leading-7 text-stone-600">{t('siteManage.emptyUsersDescription')}</p>
                <Button asChild variant="outline" className="mt-5 rounded-full border-stone-300 bg-white">
                  <Link to="/">{t('siteManage.backToDashboard')}</Link>
                </Button>
              </div>
            ) : (
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>{t('common.username')}</TableHead>
                    <TableHead>{t('common.displayName')}</TableHead>
                    <TableHead>{t('common.email')}</TableHead>
                    <TableHead>{t('common.settings')}</TableHead>
                    <TableHead>{t('siteManage.siteManager')}</TableHead>
                    <TableHead>{t('siteManage.problemManager')}</TableHead>
                    <TableHead className="text-right">{t('common.actions')}</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {users.map((listedUser) => (
                    <TableRow key={usernameValue(listedUser.username)}>
                      <TableCell className="font-medium text-stone-900">
                        {usernameValue(listedUser.username)}
                      </TableCell>
                      <TableCell>{displayNameValue(listedUser.displayName)}</TableCell>
                      <TableCell>{emailAddressValue(listedUser.email)}</TableCell>
                      <TableCell>
                        <Button asChild variant="outline" size="sm" className="rounded-full border-stone-300 bg-white">
                          <Link to={`/user/${usernameValue(listedUser.username)}/settings`}>
                            {t('siteManage.openSettings')}
                          </Link>
                        </Button>
                      </TableCell>
                      <TableCell>
                        <Checkbox
                          checked={listedUser.siteManager}
                          disabled={
                            updatingUsername !== null || deletingUsername !== null || isProtectedAdmin(listedUser)
                          }
                          aria-label="Site manager"
                          onCheckedChange={(checked) => {
                            if (siteManagerSession) {
                              void savePermissions(listedUser, {
                                siteManager: checked === true,
                                problemManager: listedUser.problemManager,
                              })
                            }
                          }}
                        />
                      </TableCell>
                      <TableCell>
                        <Checkbox
                          checked={listedUser.problemManager}
                          disabled={
                            updatingUsername !== null || deletingUsername !== null || isProtectedAdmin(listedUser)
                          }
                          aria-label="Problem manager"
                          onCheckedChange={(checked) => {
                            if (siteManagerSession) {
                              void savePermissions(listedUser, {
                                siteManager: listedUser.siteManager,
                                problemManager: checked === true,
                              })
                            }
                          }}
                        />
                      </TableCell>
                      <TableCell className="text-right">
                        <ConfirmActionDialog
                          title={t('siteManage.deleteUserTitle')}
                          description={t('siteManage.deleteUserDescription', { username: usernameValue(listedUser.username) })}
                          confirmLabel={t('siteManage.deleteUserAction')}
                          destructive
                          onConfirm={() => {
                            void deleteUser(listedUser)
                          }}
                          trigger={
                            <Button
                              type="button"
                              variant="outline"
                              size="sm"
                              className="size-8 rounded-full border-rose-300 bg-white p-0 text-rose-700 hover:bg-rose-50 hover:text-rose-800"
                              aria-label={`Delete ${usernameValue(listedUser.username)}`}
                              disabled={
                                updatingUsername !== null || deletingUsername !== null || isProtectedAdmin(listedUser)
                              }
                            >
                              <Trash2 className="size-4" />
                            </Button>
                          }
                        />
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            )}
          </CardContent>
        </Card>

        <Card className="mt-6 border-stone-200 bg-white shadow-[0_24px_60px_rgba(28,25,23,0.08)]">
          <CardHeader>
            <div className="flex items-center gap-3">
              <div className="flex size-12 items-center justify-center rounded-2xl bg-amber-100 text-amber-700">
                <Cpu className="size-5" />
              </div>
              <div>
                <CardTitle className="text-xl text-stone-950">{t('siteManage.judgersTitle')}</CardTitle>
                <CardDescription>{t('siteManage.judgersDescription')}</CardDescription>
              </div>
            </div>
          </CardHeader>
          <CardContent className="space-y-4">
            {judgerListError ? (
              <Alert variant="destructive" className="rounded-2xl border-rose-200 bg-rose-50/95">
                <AlertDescription className="text-rose-700">{judgerListError}</AlertDescription>
              </Alert>
            ) : null}
            {isLoadingJudgers ? (
              <p className="text-sm text-stone-500">{t('siteManage.loadingJudgers')}</p>
            ) : judgers.length === 0 ? (
              <div className="rounded-3xl border border-dashed border-stone-300 bg-stone-50 px-6 py-10 text-center">
                <p className="text-base font-medium text-stone-900">{t('siteManage.emptyJudgersTitle')}</p>
                <p className="mt-2 text-sm leading-7 text-stone-600">{t('siteManage.emptyJudgersDescription')}</p>
              </div>
            ) : (
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>{t('siteManage.judgerId')}</TableHead>
                    <TableHead>{t('siteManage.prefix')}</TableHead>
                    <TableHead>{t('siteManage.host')}</TableHead>
                    <TableHead>{t('siteManage.processId')}</TableHead>
                    <TableHead>{t('siteManage.languages')}</TableHead>
                    <TableHead>{t('siteManage.registeredAt')}</TableHead>
                    <TableHead>{t('siteManage.lastHeartbeat')}</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {judgers.map((judger) => (
                    <TableRow key={judger.judgerId}>
                      <TableCell className="font-medium text-stone-900">{judger.judgerId}</TableCell>
                      <TableCell>{judger.requestedPrefix}</TableCell>
                      <TableCell>{judger.host}</TableCell>
                      <TableCell>{judger.processId ?? t('siteManage.notAvailable')}</TableCell>
                      <TableCell>{judger.supportedLanguages.join(', ') || t('siteManage.notAvailable')}</TableCell>
                      <TableCell>{new Date(judger.registeredAt).toLocaleString()}</TableCell>
                      <TableCell>{new Date(judger.lastHeartbeatAt).toLocaleString()}</TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            )}
          </CardContent>
        </Card>
      </section>
    </main>
  )
}
