import type { ReactNode } from 'react'
import { Alert, AlertDescription } from '@/components/ui/alert'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import type { SessionResponse } from '@/objects/auth/response/SessionResponse'
import { emailAddressValue } from '@/objects/auth/EmailAddress'
import { displayNameValue } from '@/objects/user/DisplayName'
import { usernameValue } from '@/objects/user/Username'
import type { Username } from '@/objects/user/Username'
import { UserAvatar } from '@/pages/components/UserAvatar'
import { useI18n } from '@/system/i18n/use-i18n'

type UserProfileOverviewCardProps = {
  icon: ReactNode
  title: string
  description: string
  user: SessionResponse | null
  fallbackUsername: Username
  noticeMessage?: string | null
  loadingMessage: string
  children?: ReactNode
}

export function UserProfileOverviewCard({
  icon,
  title,
  description,
  user,
  fallbackUsername,
  noticeMessage,
  loadingMessage,
  children,
}: UserProfileOverviewCardProps) {
  const { t } = useI18n()

  return (
    <Card className="border-slate-200 bg-white shadow-[0_24px_60px_rgba(15,23,42,0.08)]">
      <CardHeader>
        <div className="flex items-center gap-3">
          <div className="flex size-12 items-center justify-center rounded-2xl bg-violet-100 text-violet-700">
            {icon}
          </div>
          <div>
            <CardTitle className="text-xl text-slate-950">{title}</CardTitle>
            <CardDescription>{description}</CardDescription>
          </div>
        </div>
      </CardHeader>
      <CardContent className="space-y-5">
        {noticeMessage ? (
          <Alert className="rounded-2xl border-violet-200 bg-violet-50/95">
            <AlertDescription className="text-violet-700">{noticeMessage}</AlertDescription>
          </Alert>
        ) : null}
        {!user ? (
          <Alert className="rounded-2xl border-slate-200 bg-slate-50/95">
            <AlertDescription className="text-slate-700">{loadingMessage}</AlertDescription>
          </Alert>
        ) : null}
        <div className="grid gap-4 sm:grid-cols-2">
          {user ? (
            <div className="rounded-2xl bg-slate-50 p-5 sm:col-span-2">
              <div className="flex items-center gap-4">
                <UserAvatar avatarUrl={user.avatarUrl} className="size-16" displayName={user.displayName} fallbackClassName="text-lg" />
                <div className="min-w-0">
                  <p className="text-sm text-slate-500">{t('userSettings.avatarTitle')}</p>
                  <p className="mt-2 text-lg font-semibold text-slate-900">
                    {displayNameValue(user.displayName)}
                  </p>
                </div>
              </div>
            </div>
          ) : null}
          <div className="rounded-2xl bg-slate-50 p-5">
            <p className="text-sm text-slate-500">{t('common.displayName')}</p>
            <p className="mt-2 text-lg font-semibold text-slate-900">
              {user ? displayNameValue(user.displayName) : t('common.loading')}
            </p>
          </div>
          <div className="rounded-2xl bg-slate-50 p-5">
            <p className="text-sm text-slate-500">{t('common.username')}</p>
            <p className="mt-2 text-lg font-semibold text-slate-900">
              {user ? usernameValue(user.username) : usernameValue(fallbackUsername)}
            </p>
          </div>
        </div>

        <div className="rounded-2xl bg-slate-50 p-5">
          <p className="text-sm text-slate-500">{t('common.email')}</p>
          <p className="mt-2 text-lg font-semibold text-slate-900">
            {user ? emailAddressValue(user.email) : t('common.loading')}
          </p>
        </div>

        {children}
      </CardContent>
    </Card>
  )
}
