import { useEffect, useState } from 'react'
import { Navigate, useLocation, useParams } from 'react-router-dom'
import { Ban, LockKeyhole, Search, Settings, SlidersHorizontal, UserRoundPen } from 'lucide-react'

import { Alert, AlertDescription } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { Switch } from '@/components/ui/switch'
import { UserAccountPageShell } from '@/features/auth/components/user-account-page-shell'
import { UserPermissionsCard } from '@/features/auth/components/user-permissions-card'
import { UserProfileOverviewCard } from '@/features/auth/components/user-profile-overview-card'
import { displayNameValue, usernameValue, type UserIdentity } from '@/features/auth/domain/auth'
import { addMessageBlock, listMessageBlocks, removeMessageBlock } from '@/features/message/api/message-client'
import { usePageTitle } from '@/shared/hooks/use-page-title'
import { useSessionGuard } from '@/features/auth/hooks/use-session-guard'
import { useUserSettingsModel } from '@/features/auth/hooks/use-user-settings-model'
import { listUserSuggestions } from '@/features/user/api/user-client'
import { HttpClientError } from '@/shared/api/http-client'
import { useI18n } from '@/shared/i18n/i18n'

export function UserSettingsPage() {
  const { t } = useI18n()
  usePageTitle(t('userSettings.pageTitle'))
  const [blockSuggestions, setBlockSuggestions] = useState<UserIdentity[]>([])
  const [blockSearch, setBlockSearch] = useState('')
  const [blockErrorMessage, setBlockErrorMessage] = useState('')
  const [isUpdatingBlocks, setIsUpdatingBlocks] = useState(false)
  const [blockedUsers, setBlockedUsers] = useState<import('@/features/message/domain/message').MessageBlockEntry[]>([])
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

  if (modelNavigationIntent) {
    return <Navigate replace={modelNavigationIntent.replace} to={modelNavigationIntent.to} />
  }

  useEffect(() => {
    if (!isEditingOwnSettings) {
      setBlockedUsers([])
      setBlockSearch('')
      setBlockSuggestions([])
      setBlockErrorMessage('')
      return
    }

    void listMessageBlocks()
      .then((entries) => {
        setBlockedUsers(entries)
        setBlockErrorMessage('')
      })
      .catch((error) => {
        setBlockErrorMessage(error instanceof HttpClientError ? error.message : t('messages.blockLoadFailed'))
      })
  }, [isEditingOwnSettings, t])

  useEffect(() => {
    if (!isEditingOwnSettings || !blockSearch.trim()) {
      setBlockSuggestions([])
      return
    }

    const timeoutId = window.setTimeout(() => {
      void listUserSuggestions(blockSearch)
        .then((items) => {
          setBlockSuggestions(items.filter((item) => item.username !== viewer.username))
        })
        .catch((error) => {
          setBlockSuggestions([])
          setBlockErrorMessage(error instanceof HttpClientError ? error.message : t('messages.blockActionFailed'))
        })
    }, 150)

    return () => window.clearTimeout(timeoutId)
  }, [blockSearch, isEditingOwnSettings, t, viewer.username])

  useEffect(() => {
    if (!isEditingOwnSettings || hash !== '#message-blocks') {
      return
    }

    const frameId = window.requestAnimationFrame(() => {
      document.getElementById('message-blocks')?.scrollIntoView({ behavior: 'smooth', block: 'start' })
    })

    return () => window.cancelAnimationFrame(frameId)
  }, [hash, isEditingOwnSettings])

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

      <Card className="border-slate-200 bg-white shadow-[0_24px_60px_rgba(15,23,42,0.08)]">
        <CardHeader>
          <div className="flex items-center gap-3">
            <div className="flex size-12 items-center justify-center rounded-2xl bg-violet-100 text-violet-700">
              <UserRoundPen className="size-5" />
            </div>
            <div>
              <CardTitle className="text-xl text-slate-950">{t('userSettings.profileFormTitle')}</CardTitle>
              <CardDescription>{t('userSettings.profileFormDescription')}</CardDescription>
            </div>
          </div>
        </CardHeader>
        <CardContent className="space-y-5">
          {sections.profile.errorMessage ? (
            <Alert variant="destructive" className="rounded-2xl border-rose-200 bg-rose-50/95">
              <AlertDescription className="text-rose-700">{sections.profile.errorMessage}</AlertDescription>
            </Alert>
          ) : null}
          {sections.profile.successMessage ? (
            <Alert className="rounded-2xl border-emerald-200 bg-emerald-50/95">
              <AlertDescription className="text-emerald-700">{sections.profile.successMessage}</AlertDescription>
            </Alert>
          ) : null}
          <div className="space-y-2">
            <Label htmlFor="settings-display-name">{t('common.displayName')}</Label>
            <Input
              id="settings-display-name"
              value={displayName}
              onChange={(event) => setDisplayName(event.target.value)}
            />
          </div>
          <div className="space-y-2">
            <Button
              type="button"
              disabled={sections.profile.isSubmitting || !displayedUser}
              className="rounded-2xl bg-violet-300 text-violet-950 hover:bg-violet-400"
              onClick={() => {
                void submit('profile')
              }}
            >
              {sections.profile.isSubmitting ? t('userSettings.saving') : t('userSettings.save')}
            </Button>
          </div>
        </CardContent>
      </Card>

      <Card className="border-slate-200 bg-white shadow-[0_24px_60px_rgba(15,23,42,0.08)]">
        <CardHeader>
          <div className="flex items-center gap-3">
            <div className="flex size-12 items-center justify-center rounded-2xl bg-violet-100 text-violet-700">
              <SlidersHorizontal className="size-5" />
            </div>
            <div>
              <CardTitle className="text-xl text-slate-950">{t('userSettings.preferencesTitle')}</CardTitle>
              <CardDescription>{t('userSettings.preferencesDescription')}</CardDescription>
            </div>
          </div>
        </CardHeader>
        <CardContent className="space-y-5">
          {sections.preferences.errorMessage ? (
            <Alert variant="destructive" className="rounded-2xl border-rose-200 bg-rose-50/95">
              <AlertDescription className="text-rose-700">{sections.preferences.errorMessage}</AlertDescription>
            </Alert>
          ) : null}
          {sections.preferences.successMessage ? (
            <Alert className="rounded-2xl border-emerald-200 bg-emerald-50/95">
              <AlertDescription className="text-emerald-700">{sections.preferences.successMessage}</AlertDescription>
            </Alert>
          ) : null}
          <div className="space-y-2">
            <Label htmlFor="settings-display-mode">{t('userSettings.displayMode')}</Label>
            <Select value={displayMode} onValueChange={(value) => setDisplayMode(value as typeof displayMode)}>
              <SelectTrigger id="settings-display-mode" className="rounded-2xl border-slate-300 bg-white">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="display_name">{t('userSettings.displayMode.displayName')}</SelectItem>
                <SelectItem value="username">{t('userSettings.displayMode.username')}</SelectItem>
                <SelectItem value="display_name_with_username">
                  {t('userSettings.displayMode.displayNameWithUsername')}
                </SelectItem>
              </SelectContent>
            </Select>
            <p className="text-sm text-slate-500">{t('userSettings.displayModeHelp')}</p>
          </div>

          <div className="space-y-2">
            <Label htmlFor="settings-locale">{t('userSettings.locale')}</Label>
            <Select value={locale} onValueChange={(value) => setLocale(value as typeof locale)}>
              <SelectTrigger id="settings-locale" className="rounded-2xl border-slate-300 bg-white">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="en">{t('common.language.en')}</SelectItem>
                <SelectItem value="zh-CN">{t('common.language.zh-CN')}</SelectItem>
              </SelectContent>
            </Select>
            <p className="text-sm text-slate-500">{t('userSettings.localeHelp')}</p>
          </div>

          <div className="space-y-2">
            <Label htmlFor="settings-problem-title-display-mode">{t('userSettings.problemTitleDisplayMode')}</Label>
            <Select
              value={problemTitleDisplayMode}
              onValueChange={(value) => setProblemTitleDisplayMode(value as typeof problemTitleDisplayMode)}
            >
              <SelectTrigger
                id="settings-problem-title-display-mode"
                className="rounded-2xl border-slate-300 bg-white"
              >
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="title">{t('userSettings.problemTitleDisplayMode.title')}</SelectItem>
                <SelectItem value="slug">{t('userSettings.problemTitleDisplayMode.slug')}</SelectItem>
                <SelectItem value="title_with_slug">
                  {t('userSettings.problemTitleDisplayMode.titleWithSlug')}
                </SelectItem>
              </SelectContent>
            </Select>
            <p className="text-sm text-slate-500">{t('userSettings.problemTitleDisplayModeHelp')}</p>
          </div>
          <div className="flex items-start justify-between gap-4 rounded-2xl border border-slate-200 bg-slate-50 px-4 py-3">
            <div className="space-y-1">
              <Label htmlFor="settings-auto-mark-message-read">{t('userSettings.autoMarkMessageRead')}</Label>
              <p className="text-sm text-slate-500">{t('userSettings.autoMarkMessageReadHelp')}</p>
            </div>
            <Switch
              id="settings-auto-mark-message-read"
              checked={autoMarkMessageRead}
              onCheckedChange={setAutoMarkMessageRead}
            />
          </div>
          <Button
            type="button"
            disabled={sections.preferences.isSubmitting || !displayedUser}
            className="rounded-2xl bg-violet-300 text-violet-950 hover:bg-violet-400"
            onClick={() => {
              void submit('preferences')
            }}
          >
            {sections.preferences.isSubmitting ? t('userSettings.saving') : t('userSettings.save')}
          </Button>
        </CardContent>
      </Card>

      <Card className="border-slate-200 bg-white shadow-[0_24px_60px_rgba(15,23,42,0.08)]">
        <CardHeader>
          <div className="flex items-center gap-3">
            <div className="flex size-12 items-center justify-center rounded-2xl bg-violet-100 text-violet-700">
              <LockKeyhole className="size-5" />
            </div>
            <div>
              <CardTitle className="text-xl text-slate-950">{t('userSettings.accountTitle')}</CardTitle>
              <CardDescription>{t('userSettings.accountDescription')}</CardDescription>
            </div>
          </div>
        </CardHeader>
        <CardContent className="space-y-5">
          {sections.account.errorMessage ? (
            <Alert variant="destructive" className="rounded-2xl border-rose-200 bg-rose-50/95">
              <AlertDescription className="text-rose-700">{sections.account.errorMessage}</AlertDescription>
            </Alert>
          ) : null}
          {sections.account.successMessage ? (
            <Alert className="rounded-2xl border-emerald-200 bg-emerald-50/95">
              <AlertDescription className="text-emerald-700">{sections.account.successMessage}</AlertDescription>
            </Alert>
          ) : null}
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

          <Button
            type="button"
            disabled={sections.account.isSubmitting || !displayedUser}
            className="rounded-2xl bg-violet-300 text-violet-950 hover:bg-violet-400"
            onClick={() => {
              void submit('account')
            }}
          >
            {sections.account.isSubmitting ? t('userSettings.saving') : t('userSettings.save')}
          </Button>
        </CardContent>
      </Card>

      <UserPermissionsCard
        description={t('userSettings.permissionsDescription')}
        title={t('userSettings.permissionsTitle')}
        user={displayedUser}
      />

      {isEditingOwnSettings ? (
        <Card id="message-blocks" className="scroll-mt-28 border-slate-200 bg-white shadow-[0_24px_60px_rgba(15,23,42,0.08)]">
          <CardHeader>
            <div className="flex items-center gap-3">
              <div className="flex size-12 items-center justify-center rounded-2xl bg-violet-100 text-violet-700">
                <Ban className="size-5" />
              </div>
              <div>
                <CardTitle className="text-xl text-slate-950">{t('messages.blockListTitle')}</CardTitle>
                <CardDescription>{t('messages.blockListDescription')}</CardDescription>
              </div>
            </div>
          </CardHeader>
          <CardContent className="space-y-5">
            {blockErrorMessage ? (
              <Alert variant="destructive" className="rounded-2xl border-rose-200 bg-rose-50/95">
                <AlertDescription className="text-rose-700">{blockErrorMessage}</AlertDescription>
              </Alert>
            ) : null}
            <div className="space-y-2">
              <div className="relative">
                <Search className="pointer-events-none absolute left-3 top-3 size-4 text-slate-400" />
                <Input
                  className="rounded-2xl border-slate-300 bg-white pl-10"
                  value={blockSearch}
                  onChange={(event) => setBlockSearch(event.target.value)}
                  placeholder={t('messages.blockSearchPlaceholder')}
                />
              </div>
              <div className="space-y-2">
                {blockSuggestions.map((suggestion) => (
                  <button
                    key={usernameValue(suggestion.username)}
                    type="button"
                    className="flex w-full items-center justify-between rounded-2xl border border-slate-200 bg-slate-50 px-4 py-3 text-left transition hover:border-rose-300 hover:bg-rose-50"
                    disabled={isUpdatingBlocks}
                    onClick={() => {
                      setIsUpdatingBlocks(true)
                      void addMessageBlock(suggestion.username)
                        .then((entry) => {
                          setBlockedUsers((current) => [entry, ...current.filter((item) => item.user.username !== entry.user.username)])
                          setBlockSearch('')
                          setBlockSuggestions([])
                          setBlockErrorMessage('')
                        })
                        .catch((error) => {
                          setBlockErrorMessage(error instanceof HttpClientError ? error.message : t('messages.blockActionFailed'))
                        })
                        .finally(() => setIsUpdatingBlocks(false))
                    }}
                  >
                    <div>
                      <p className="font-medium text-slate-950">{suggestion.displayName}</p>
                      <p className="text-sm text-slate-600">@{usernameValue(suggestion.username)}</p>
                    </div>
                    <span className="text-sm font-medium text-rose-700">{t('messages.blockAdd')}</span>
                  </button>
                ))}
              </div>
            </div>

            <div className="space-y-3">
              {blockedUsers.length === 0 ? <p className="text-sm text-slate-500">{t('messages.blockEmpty')}</p> : null}
              {blockedUsers.map((entry) => (
                <div
                  key={usernameValue(entry.user.username)}
                  className="flex items-center justify-between rounded-2xl border border-slate-200 bg-slate-50 px-4 py-3"
                >
                  <div>
                    <p className="font-medium text-slate-950">{entry.user.displayName}</p>
                    <p className="text-sm text-slate-600">@{usernameValue(entry.user.username)}</p>
                  </div>
                  <Button
                    type="button"
                    variant="outline"
                    className="rounded-2xl border-rose-300 bg-white text-rose-950"
                    disabled={isUpdatingBlocks}
                    onClick={() => {
                      setIsUpdatingBlocks(true)
                      void removeMessageBlock(entry.user.username)
                        .then(() => {
                          setBlockedUsers((current) => current.filter((item) => item.user.username !== entry.user.username))
                          setBlockErrorMessage('')
                        })
                        .catch((error) => {
                          setBlockErrorMessage(error instanceof HttpClientError ? error.message : t('messages.blockActionFailed'))
                        })
                        .finally(() => setIsUpdatingBlocks(false))
                    }}
                  >
                    {t('messages.blockRemove')}
                  </Button>
                </div>
              ))}
            </div>
          </CardContent>
        </Card>
      ) : null}
    </UserAccountPageShell>
  )
}
