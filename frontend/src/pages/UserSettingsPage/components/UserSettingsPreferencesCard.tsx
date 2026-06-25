import { SlidersHorizontal } from 'lucide-react'

import { Alert, AlertDescription } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Label } from '@/components/ui/label'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { Switch } from '@/components/ui/switch'
import type { SessionResponse } from '@/objects/auth/response/SessionResponse'
import type { ProblemTitleDisplayMode } from '@/objects/problem/ProblemTitleDisplayMode'
import type { UserDisplayMode } from '@/objects/user/UserDisplayMode'
import type { UserLocale } from '@/objects/user/UserLocale'
import type { UserSettingsSectionState } from '../functions/UserSettingsState'
import { useI18n } from '@/system/i18n/use-i18n'

/**
 * 用户偏好设置卡片状态，包含展示目标和保存结果。
 */
type UserSettingsPreferencesState = {
  displayedUser: SessionResponse | null
  section: UserSettingsSectionState
}

type UserSettingsPreferencesDraft = {
  autoMarkMessageRead: boolean
  displayMode: UserDisplayMode
  locale: UserLocale
  problemTitleDisplayMode: ProblemTitleDisplayMode
}

type UserSettingsPreferencesActions = {
  setAutoMarkMessageRead: (value: boolean) => void
  setDisplayMode: (value: UserDisplayMode) => void
  setLocale: (value: UserLocale) => void
  setProblemTitleDisplayMode: (value: ProblemTitleDisplayMode) => void
  submit: () => void
}

type UserSettingsPreferencesCardProps = {
  state: UserSettingsPreferencesState
  draft: UserSettingsPreferencesDraft
  actions: UserSettingsPreferencesActions
}

/**
 * 用户偏好设置卡片，渲染显示模式、语言、题名模式和消息自动已读选项。
 */
export function UserSettingsPreferencesCard({
  state,
  draft,
  actions,
}: UserSettingsPreferencesCardProps) {
  const { t } = useI18n()

  return (
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
          <Label htmlFor="settings-display-mode">{t('userSettings.displayMode')}</Label>
          {/* 注意：以下偏好 Select 的选项值均由本组件内 SelectItem 字面值限定，Radix 回调统一为 string。 */}
          <Select value={draft.displayMode} onValueChange={(value) => actions.setDisplayMode(value as UserDisplayMode)}>
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
          <Select value={draft.locale} onValueChange={(value) => actions.setLocale(value as UserLocale)}>
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
            value={draft.problemTitleDisplayMode}
            onValueChange={(value) => actions.setProblemTitleDisplayMode(value as ProblemTitleDisplayMode)}
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
            checked={draft.autoMarkMessageRead}
            onCheckedChange={actions.setAutoMarkMessageRead}
          />
        </div>
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
