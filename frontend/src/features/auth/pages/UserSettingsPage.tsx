import { Navigate, useParams, useSearchParams } from 'react-router-dom'
import { LockKeyhole, Settings, ShieldCheck } from 'lucide-react'

import { Alert, AlertDescription } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Checkbox } from '@/components/ui/checkbox'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import {
  displayNameValue,
  usernameValue,
} from '@/features/auth/domain/auth'
import { usePageTitle } from '@/shared/hooks/use-page-title'
import { useSessionGuard } from '@/features/auth/hooks/use-session-guard'
import { useUserSettingsModel } from '@/features/auth/hooks/use-user-settings-model'
import { AncestorNavigation } from '@/shared/components/ancestor-navigation'
import { useI18n } from '@/shared/i18n/i18n'

export function UserSettingsPage() {
  const { t } = useI18n()
  usePageTitle(t('userSettings.pageTitle'))
  const [searchParams] = useSearchParams()
  const { username: routeUsername } = useParams<{ username: string }>()
  const { session: viewer, setSession: setViewer, navigationIntent: guardNavigationIntent } =
    useSessionGuard()
  const notice = searchParams.get('notice')
  const noticeMessage =
    notice === 'route-corrected'
      ? t('userSettings.routeCorrected')
      : null

  if (guardNavigationIntent) {
    return <Navigate replace={guardNavigationIntent.replace} to={guardNavigationIntent.to} />
  }

  if (!viewer) {
    return <Navigate replace to="/login" />
  }

  const {
    displayedUser,
    displayName,
    email,
    currentPassword,
    newPassword,
    confirmNewPassword,
    errorMessage,
    successMessage,
    isSubmitting,
    isEditingOwnSettings,
    targetUsername,
    navigationIntent: modelNavigationIntent,
    setDisplayName,
    setEmail,
    setCurrentPassword,
    setNewPassword,
    setConfirmNewPassword,
    submit,
  } = useUserSettingsModel({
    viewer,
    routeUsername,
    setViewer,
  })

  if (modelNavigationIntent) {
    return <Navigate replace={modelNavigationIntent.replace} to={modelNavigationIntent.to} />
  }

  return (
    <main className="min-h-screen bg-[linear-gradient(180deg,#f7fafc_0%,#edf2f7_100%)] px-6 py-12 sm:px-8">
      <section className="mx-auto max-w-6xl">
        <div className="mb-8 flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
          <div className="space-y-2">
            <p className="text-sm uppercase tracking-[0.25em] text-slate-500">{t('common.siteName')}</p>
            <h1 className="font-['Georgia'] text-4xl font-semibold tracking-tight text-slate-950">
              {t('userSettings.heading')}
            </h1>
            <p className="text-sm text-slate-600">
              {displayedUser
                ? isEditingOwnSettings
                  ? t('userSettings.managingOwn', {
                      displayName: displayNameValue(displayedUser.displayName),
                      username: usernameValue(displayedUser.username),
                    })
                  : t('userSettings.managingOther', {
                      displayName: displayNameValue(displayedUser.displayName),
                      username: usernameValue(displayedUser.username),
                    })
                : t('userSettings.loadingFor', { username: targetUsername })}
            </p>
          </div>

          <AncestorNavigation />
        </div>

        <div className="grid gap-6 lg:grid-cols-[1.05fr_0.95fr]">
          <Card className="border-slate-200 bg-white shadow-[0_24px_60px_rgba(15,23,42,0.08)]">
            <CardHeader>
              <div className="flex items-center gap-3">
                <div className="flex size-12 items-center justify-center rounded-2xl bg-violet-100 text-violet-700">
                  <Settings className="size-5" />
                </div>
                <div>
                  <CardTitle className="text-xl text-slate-950">{t('userSettings.profileTitle')}</CardTitle>
                  <CardDescription>{t('userSettings.profileDescription')}</CardDescription>
                </div>
              </div>
            </CardHeader>
            <CardContent className="space-y-5">
              {noticeMessage ? (
                <Alert className="rounded-2xl border-violet-200 bg-violet-50/95">
                  <AlertDescription className="text-violet-700">{noticeMessage}</AlertDescription>
                </Alert>
              ) : null}
              {!displayedUser ? (
                <Alert className="rounded-2xl border-slate-200 bg-slate-50/95">
                  <AlertDescription className="text-slate-700">{t('userSettings.loadingSelected')}</AlertDescription>
                </Alert>
              ) : null}
              <div className="rounded-2xl bg-slate-50 p-5">
                <p className="text-sm text-slate-500">{t('common.username')}</p>
                <p className="mt-2 text-lg font-semibold text-slate-900">
                  {displayedUser ? usernameValue(displayedUser.username) : targetUsername}
                </p>
              </div>

              <div className="space-y-2">
                <Label htmlFor="settings-display-name">{t('common.displayName')}</Label>
                <Input
                  id="settings-display-name"
                  value={displayName}
                  onChange={(event) => setDisplayName(event.target.value)}
                />
              </div>

              <div className="space-y-2">
                <Label htmlFor="settings-email">{t('common.email')}</Label>
                <Input
                  id="settings-email"
                  value={email}
                  onChange={(event) => setEmail(event.target.value)}
                />
              </div>

              <div className="grid gap-4 sm:grid-cols-2">
                <div className="space-y-2">
                  <Label htmlFor="settings-new-password">{t('userSettings.newPassword')}</Label>
                  <Input
                    id="settings-new-password"
                    type="password"
                    value={newPassword}
                    onChange={(event) => setNewPassword(event.target.value)}
                  />
                </div>
                <div className="space-y-2">
                  <Label htmlFor="settings-confirm-password">{t('userSettings.confirmNewPassword')}</Label>
                  <Input
                    id="settings-confirm-password"
                    type="password"
                    value={confirmNewPassword}
                    onChange={(event) => setConfirmNewPassword(event.target.value)}
                  />
                </div>
              </div>

              {isEditingOwnSettings ? (
                <div className="rounded-2xl border border-slate-200 bg-slate-50 p-5">
                  <div className="mb-3 flex items-center gap-2 text-slate-800">
                    <LockKeyhole className="size-4" />
                    <p className="font-medium">{t('userSettings.currentPasswordTitle')}</p>
                  </div>
                  <div className="space-y-2">
                    <Label htmlFor="settings-current-password">{t('userSettings.currentPassword')}</Label>
                    <Input
                      id="settings-current-password"
                      type="password"
                      value={currentPassword}
                      onChange={(event) => setCurrentPassword(event.target.value)}
                    />
                  </div>
                </div>
              ) : (
                <div className="rounded-2xl border border-amber-200 bg-amber-50 p-5 text-sm leading-7 text-amber-900">
                  {t('userSettings.siteManagerNotice')}
                </div>
              )}

              {errorMessage ? (
                <Alert variant="destructive" className="rounded-2xl border-rose-200 bg-rose-50/95">
                  <AlertDescription className="text-rose-700">{errorMessage}</AlertDescription>
                </Alert>
              ) : null}
              {successMessage ? (
                <Alert className="rounded-2xl border-emerald-200 bg-emerald-50/95">
                  <AlertDescription className="text-emerald-700">{successMessage}</AlertDescription>
                </Alert>
              ) : null}

              <Button
                type="button"
                disabled={isSubmitting || !displayedUser}
                className="rounded-2xl bg-violet-300 text-violet-950 hover:bg-violet-400"
                onClick={() => {
                  void submit()
                }}
              >
                {isSubmitting ? t('userSettings.saving') : t('userSettings.save')}
              </Button>
            </CardContent>
          </Card>

          <Card className="border-slate-200 bg-white shadow-[0_24px_60px_rgba(15,23,42,0.08)]">
            <CardHeader>
              <div className="flex items-center gap-3">
                <div className="flex size-12 items-center justify-center rounded-2xl bg-violet-100 text-violet-700">
                  <ShieldCheck className="size-5" />
                </div>
                <div>
                  <CardTitle className="text-xl text-slate-950">{t('userSettings.permissionsTitle')}</CardTitle>
                  <CardDescription>{t('userSettings.permissionsDescription')}</CardDescription>
                </div>
              </div>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="flex items-center justify-between rounded-2xl bg-slate-50 px-5 py-4">
                <div>
                  <p className="font-medium text-slate-900">{t('siteManage.siteManager')}</p>
                  <p className="text-sm text-slate-500">{t('userSettings.siteManagerDescription')}</p>
                </div>
                <Checkbox checked={displayedUser?.siteManager ?? false} disabled aria-label="Site manager permission" />
              </div>
              <div className="flex items-center justify-between rounded-2xl bg-slate-50 px-5 py-4">
                <div>
                  <p className="font-medium text-slate-900">{t('siteManage.problemManager')}</p>
                  <p className="text-sm text-slate-500">{t('userSettings.problemManagerDescription')}</p>
                </div>
                <Checkbox checked={displayedUser?.problemManager ?? false} disabled aria-label="Problem manager permission" />
              </div>
            </CardContent>
          </Card>
        </div>
      </section>
    </main>
  )
}
