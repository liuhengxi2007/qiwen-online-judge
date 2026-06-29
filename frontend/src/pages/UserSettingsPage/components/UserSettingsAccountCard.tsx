import { LockKeyhole } from 'lucide-react'

import { Alert, AlertDescription } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import type { SessionResponse } from '@/objects/auth/response/SessionResponse'
import type { UserSettingsSectionState } from '../functions/UserSettingsState'
import { useI18n } from '@/system/i18n/use-i18n'

/**
 * 用户账户设置卡片状态，包含展示目标、编辑权限和保存结果。
 */
type UserSettingsAccountState = {
  displayedUser: SessionResponse | null
  isEditingOwnSettings: boolean
  section: UserSettingsSectionState
}

type UserSettingsAccountDraft = {
  confirmNewPassword: string
  currentPassword: string
  email: string
  newPassword: string
}

type UserSettingsAccountActions = {
  setConfirmNewPassword: (value: string) => void
  setCurrentPassword: (value: string) => void
  setEmail: (value: string) => void
  setNewPassword: (value: string) => void
  submit: () => void
}

type UserSettingsAccountCardProps = {
  state: UserSettingsAccountState
  draft: UserSettingsAccountDraft
  actions: UserSettingsAccountActions
}

/**
 * 用户账户设置卡片，渲染邮箱和密码变更表单。
 */
export function UserSettingsAccountCard({
  state,
  draft,
  actions,
}: UserSettingsAccountCardProps) {
  const { t } = useI18n()

  return (
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
        {state.section.errorMessage ? (
          <Alert variant="destructive" className="rounded-2xl border-rose-200 bg-rose-50/95">
            <AlertDescription className="text-rose-700">{state.section.errorMessage}</AlertDescription>
          </Alert>
        ) : null}
        {state.section.successMessage ? (
          <Alert className="rounded-2xl border-emerald-200 bg-emerald-50/95">
            <AlertDescription className="text-emerald-700">{state.section.successMessage}</AlertDescription>
          </Alert>
        ) : null}
        <div className="space-y-2">
          <Label htmlFor="settings-email">{t('common.email')}</Label>
          <Input
            id="settings-email"
            value={draft.email}
            onChange={(event) => actions.setEmail(event.target.value)}
          />
        </div>
        <div className="grid gap-4 sm:grid-cols-2">
          <div className="space-y-2">
            <Label htmlFor="settings-new-password">{t('userSettings.newPassword')}</Label>
            <Input
              id="settings-new-password"
              type="password"
              value={draft.newPassword}
              onChange={(event) => actions.setNewPassword(event.target.value)}
            />
          </div>
          <div className="space-y-2">
            <Label htmlFor="settings-confirm-password">{t('userSettings.confirmNewPassword')}</Label>
            <Input
              id="settings-confirm-password"
              type="password"
              value={draft.confirmNewPassword}
              onChange={(event) => actions.setConfirmNewPassword(event.target.value)}
            />
          </div>
        </div>

        {state.isEditingOwnSettings ? (
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
                value={draft.currentPassword}
                onChange={(event) => actions.setCurrentPassword(event.target.value)}
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
          disabled={state.section.isSubmitting || !state.displayedUser}
          className="rounded-2xl bg-violet-300 text-violet-950 hover:bg-violet-400"
          onClick={actions.submit}
        >
          {state.section.isSubmitting ? t('userSettings.saving') : t('userSettings.save')}
        </Button>
      </CardContent>
    </Card>
  )
}
