import { Navigate, useLocation, useParams } from 'react-router-dom'
import { Settings } from 'lucide-react'

import { Alert, AlertDescription } from '@/components/ui/alert'
import type { SessionResponse } from '@/objects/auth/response/SessionResponse'
import { useSessionGuard } from '@/pages/hooks/useSessionGuard'
import { MessageBlockListCard } from './components/MessageBlockListCard'
import { UserPermissionsCard } from './components/UserPermissionsCard'
import { UserAvatarSettingsCard } from './components/UserAvatarSettingsCard'
import { UserProfileOverviewCard } from './components/UserProfileOverviewCard'
import { UserSettingsAccountCard } from './components/UserSettingsAccountCard'
import { UserSettingsPreferencesCard } from './components/UserSettingsPreferencesCard'
import { UserSettingsProfileCard } from './components/UserSettingsProfileCard'
import { displayNameValue } from '@/objects/user/DisplayName'
import { PageShell } from '@/pages/components/PageShell'
import { useUserSettingsBlockList } from './hooks/useUserSettingsBlockList'
import { useUserSettingsModel } from './hooks/useUserSettingsModel'
import { usePageTitle } from '@/pages/hooks/usePageTitle'
import { useI18n } from '@/system/i18n/use-i18n'

/**
 * 用户设置页入口，解析可选用户名并校验当前会话。
 */
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

/**
 * 用户设置页主体，组合路由权限策略、资料查询、表单模型和设置卡片。
 */
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
  const settingsModel = useUserSettingsModel({
    viewer,
    routeUsername,
    setViewer,
  })
  const blockList = useUserSettingsBlockList({
    hash,
    isEnabled: settingsModel.isEditingOwnSettings,
    viewerUsername: viewer.username,
  })

  if (settingsModel.navigationIntent) {
    return <Navigate replace={settingsModel.navigationIntent.replace} to={settingsModel.navigationIntent.to} />
  }

  return (
    <PageShell
      title={t('userSettings.heading')}
      description={
        settingsModel.displayedUser
          ? settingsModel.isEditingOwnSettings
            ? t('userSettings.managingOwn', {
                displayName: displayNameValue(settingsModel.displayedUser.displayName),
              })
            : t('userSettings.managingOther', {
                displayName: displayNameValue(settingsModel.displayedUser.displayName),
              })
          : t('userSettings.loadingFor', { username: t('common.loading') })
      }
      mainClassName="bg-[linear-gradient(180deg,#f7fafc_0%,#edf2f7_100%)]"
    >
      <div className="grid gap-6 lg:grid-cols-[1.05fr_0.95fr]">
        <UserProfileOverviewCard
          description={t('userSettings.profileDescription')}
          fallbackUsername={settingsModel.targetUsername}
          icon={<Settings className="size-5" />}
          loadingMessage={t('userSettings.loadingSelected')}
          title={t('userSettings.profileTitle')}
          user={settingsModel.displayedUser}
        />

        {settingsModel.loadErrorMessage ? (
          <Alert variant="destructive" className="rounded-2xl border-rose-200 bg-rose-50/95">
            <AlertDescription className="text-rose-700">{settingsModel.loadErrorMessage}</AlertDescription>
          </Alert>
        ) : null}

        <UserSettingsProfileCard
          displayedUser={settingsModel.displayedUser}
          displayName={settingsModel.displayName}
          section={settingsModel.sections.profile}
          setDisplayName={settingsModel.setDisplayName}
          submit={() => {
            void settingsModel.submit('profile')
          }}
        />

        <UserAvatarSettingsCard
          displayedUser={settingsModel.displayedUser}
          onUserUpdated={settingsModel.replaceDisplayedUser}
          targetUsername={settingsModel.targetUsername}
        />

        <UserSettingsPreferencesCard
          state={{
            displayedUser: settingsModel.displayedUser,
            section: settingsModel.sections.preferences,
          }}
          draft={{
            autoMarkMessageRead: settingsModel.autoMarkMessageRead,
            displayMode: settingsModel.displayMode,
            locale: settingsModel.locale,
            problemTitleDisplayMode: settingsModel.problemTitleDisplayMode,
          }}
          actions={{
            setAutoMarkMessageRead: settingsModel.setAutoMarkMessageRead,
            setDisplayMode: settingsModel.setDisplayMode,
            setLocale: settingsModel.setLocale,
            setProblemTitleDisplayMode: settingsModel.setProblemTitleDisplayMode,
            submit: () => {
              void settingsModel.submit('preferences')
            },
          }}
        />

        <UserSettingsAccountCard
          state={{
            displayedUser: settingsModel.displayedUser,
            isEditingOwnSettings: settingsModel.isEditingOwnSettings,
            section: settingsModel.sections.account,
          }}
          draft={{
            confirmNewPassword: settingsModel.confirmNewPassword,
            currentPassword: settingsModel.currentPassword,
            email: settingsModel.email,
            newPassword: settingsModel.newPassword,
          }}
          actions={{
            setConfirmNewPassword: settingsModel.setConfirmNewPassword,
            setCurrentPassword: settingsModel.setCurrentPassword,
            setEmail: settingsModel.setEmail,
            setNewPassword: settingsModel.setNewPassword,
            submit: () => {
              void settingsModel.submit('account')
            },
          }}
        />

        <UserPermissionsCard
          description={t('userSettings.permissionsDescription')}
          title={t('userSettings.permissionsTitle')}
          user={settingsModel.displayedUser}
        />

        {settingsModel.isEditingOwnSettings ? <MessageBlockListCard {...blockList} /> : null}
      </div>
    </PageShell>
  )
}
