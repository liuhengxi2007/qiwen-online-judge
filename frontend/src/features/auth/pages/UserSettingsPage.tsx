import { Navigate, useParams } from 'react-router-dom'
import { LockKeyhole, Settings } from 'lucide-react'

import { Alert, AlertDescription } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { UserAccountPageShell } from '@/features/auth/components/user-account-page-shell'
import { UserPermissionsCard } from '@/features/auth/components/user-permissions-card'
import { UserProfileOverviewCard } from '@/features/auth/components/user-profile-overview-card'
import { usePageTitle } from '@/shared/hooks/use-page-title'
import { useSessionGuard } from '@/features/auth/hooks/use-session-guard'
import { useUserSettingsModel } from '@/features/auth/hooks/use-user-settings-model'
import { useI18n } from '@/shared/i18n/i18n'

export function UserSettingsPage() {
  const { t } = useI18n()
  usePageTitle(t('userSettings.pageTitle'))
  const { username: routeUsername } = useParams<{ username: string }>()
  const { session: viewer, setSession: setViewer, navigationIntent: guardNavigationIntent } =
    useSessionGuard()

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
    <UserAccountPageShell
      heading={t('userSettings.heading')}
      subheading={
        displayedUser
          ? isEditingOwnSettings
            ? t('userSettings.managingOwn', {
                displayName: displayedUser.displayName,
                username: displayedUser.username,
              })
            : t('userSettings.managingOther', {
                displayName: displayedUser.displayName,
                username: displayedUser.username,
              })
          : t('userSettings.loadingFor', { username: targetUsername })
      }
    >
      <UserProfileOverviewCard
        description={t('userSettings.profileDescription')}
        fallbackUsername={targetUsername}
        icon={<Settings className="size-5" />}
        loadingMessage={t('userSettings.loadingSelected')}
        title={t('userSettings.profileTitle')}
        user={displayedUser}
      >
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
      </UserProfileOverviewCard>

      <UserPermissionsCard
        description={t('userSettings.permissionsDescription')}
        title={t('userSettings.permissionsTitle')}
        user={displayedUser}
      />
    </UserAccountPageShell>
  )
}
