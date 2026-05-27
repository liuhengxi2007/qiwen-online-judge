import { Navigate, useLocation, useParams } from 'react-router-dom'
import { Settings } from 'lucide-react'

import { Alert, AlertDescription } from '@/components/ui/alert'
import type { SessionResponse } from '@/objects/auth/response/SessionResponse'
import { useSessionGuard } from '@/pages/hooks/use-session-guard'
import { MessageBlockListCard } from './components/message-block-list-card'
import { UserAccountPageShell } from './components/user-account-page-shell'
import { UserPermissionsCard } from './components/user-permissions-card'
import { UserProfileOverviewCard } from './components/user-profile-overview-card'
import { UserSettingsAccountCard } from './components/user-settings-account-card'
import { UserSettingsPreferencesCard } from './components/user-settings-preferences-card'
import { UserSettingsProfileCard } from './components/user-settings-profile-card'
import { displayNameValue } from '@/objects/user/DisplayName'
import { useUserSettingsBlockList } from './hooks/use-user-settings-block-list'
import { useUserSettingsModel } from './hooks/use-user-settings-model'
import { usePageTitle } from '@/pages/hooks/use-page-title'
import { useI18n } from '@/system/i18n/use-i18n'

export function UserSettingsPage() {
  const { t } = useI18n()
  usePageTitle(t('userSettings.pageTitle'))
  const { username: routeUsername } = useParams<{ username: string }>()
  const { hash } = useLocation()
  const { session: viewer, setSession: setViewer, navigationIntent: guardNavigationIntent } =
    useSessionGuard()

  if (guardNavigationIntent) {
    return <Navigate replace={guardNavigationIntent.replace} to={guardNavigationIntent.to} />
  }

  if (!viewer) {
    return <Navigate replace to="/login" />
  }

  return <UserSettingsPageContent hash={hash} routeUsername={routeUsername} setViewer={setViewer} viewer={viewer} />
}

function UserSettingsPageContent({
  hash,
  routeUsername,
  setViewer,
  viewer,
}: {
  hash: string
  routeUsername?: string
  setViewer: (session: SessionResponse | null) => void
  viewer: SessionResponse
}) {
  const { t } = useI18n()
  const {
    displayedUser,
    displayName,
    email,
    displayMode,
    locale,
    problemTitleDisplayMode,
    autoMarkMessageRead,
    currentPassword,
    newPassword,
    confirmNewPassword,
    loadErrorMessage,
    sections,
    isEditingOwnSettings,
    targetUsername,
    navigationIntent: modelNavigationIntent,
    setDisplayName,
    setEmail,
    setDisplayMode,
    setLocale,
    setProblemTitleDisplayMode,
    setAutoMarkMessageRead,
    setCurrentPassword,
    setNewPassword,
    setConfirmNewPassword,
    submit,
  } = useUserSettingsModel({
    viewer,
    routeUsername,
    setViewer,
  })
  const blockList = useUserSettingsBlockList({
    hash,
    isEnabled: isEditingOwnSettings,
    viewerUsername: viewer.username,
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
                displayName: displayNameValue(displayedUser.displayName),
              })
            : t('userSettings.managingOther', {
                displayName: displayNameValue(displayedUser.displayName),
              })
          : t('userSettings.loadingFor', { username: t('common.loading') })
      }
    >
      <UserProfileOverviewCard
        description={t('userSettings.profileDescription')}
        fallbackUsername={targetUsername}
        icon={<Settings className="size-5" />}
        loadingMessage={t('userSettings.loadingSelected')}
        title={t('userSettings.profileTitle')}
        user={displayedUser}
      />

      {loadErrorMessage ? (
        <Alert variant="destructive" className="rounded-2xl border-rose-200 bg-rose-50/95">
          <AlertDescription className="text-rose-700">{loadErrorMessage}</AlertDescription>
        </Alert>
      ) : null}

      <UserSettingsProfileCard
        displayedUser={displayedUser}
        displayName={displayName}
        section={sections.profile}
        setDisplayName={setDisplayName}
        submit={() => {
          void submit('profile')
        }}
      />

      <UserSettingsPreferencesCard
        autoMarkMessageRead={autoMarkMessageRead}
        displayedUser={displayedUser}
        displayMode={displayMode}
        locale={locale}
        problemTitleDisplayMode={problemTitleDisplayMode}
        section={sections.preferences}
        setAutoMarkMessageRead={setAutoMarkMessageRead}
        setDisplayMode={setDisplayMode}
        setLocale={setLocale}
        setProblemTitleDisplayMode={setProblemTitleDisplayMode}
        submit={() => {
          void submit('preferences')
        }}
      />

      <UserSettingsAccountCard
        confirmNewPassword={confirmNewPassword}
        currentPassword={currentPassword}
        displayedUser={displayedUser}
        email={email}
        isEditingOwnSettings={isEditingOwnSettings}
        newPassword={newPassword}
        section={sections.account}
        setConfirmNewPassword={setConfirmNewPassword}
        setCurrentPassword={setCurrentPassword}
        setEmail={setEmail}
        setNewPassword={setNewPassword}
        submit={() => {
          void submit('account')
        }}
      />

      <UserPermissionsCard
        description={t('userSettings.permissionsDescription')}
        title={t('userSettings.permissionsTitle')}
        user={displayedUser}
      />

      {isEditingOwnSettings ? <MessageBlockListCard {...blockList} /> : null}
    </UserAccountPageShell>
  )
}
