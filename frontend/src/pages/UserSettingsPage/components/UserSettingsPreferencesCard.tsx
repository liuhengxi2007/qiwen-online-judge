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

type UserSettingsPreferencesCardProps = {
  autoMarkMessageRead: boolean
  displayedUser: SessionResponse | null
  displayMode: UserDisplayMode
  locale: UserLocale
  problemTitleDisplayMode: ProblemTitleDisplayMode
  section: UserSettingsSectionState
  setAutoMarkMessageRead: (value: boolean) => void
  setDisplayMode: (value: UserDisplayMode) => void
  setLocale: (value: UserLocale) => void
  setProblemTitleDisplayMode: (value: ProblemTitleDisplayMode) => void
  submit: () => void
}

export function UserSettingsPreferencesCard({
  autoMarkMessageRead,
  displayedUser,
  displayMode,
  locale,
  problemTitleDisplayMode,
  section,
  setAutoMarkMessageRead,
  setDisplayMode,
  setLocale,
  setProblemTitleDisplayMode,
  submit,
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
        {section.errorMessage ? (
          <Alert variant="destructive" className="rounded-2xl border-rose-200 bg-rose-50/95">
            <AlertDescription className="text-rose-700">{section.errorMessage}</AlertDescription>
          </Alert>
        ) : null}
        {section.successMessage ? (
          <Alert className="rounded-2xl border-emerald-200 bg-emerald-50/95">
            <AlertDescription className="text-emerald-700">{section.successMessage}</AlertDescription>
          </Alert>
        ) : null}
        <div className="space-y-2">
          <Label htmlFor="settings-display-mode">{t('userSettings.displayMode')}</Label>
          <Select value={displayMode} onValueChange={(value) => setDisplayMode(value as UserDisplayMode)}>
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
          <Select value={locale} onValueChange={(value) => setLocale(value as UserLocale)}>
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
            onValueChange={(value) => setProblemTitleDisplayMode(value as ProblemTitleDisplayMode)}
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
          disabled={section.isSubmitting || !displayedUser}
          className="rounded-2xl bg-violet-300 text-violet-950 hover:bg-violet-400"
          onClick={submit}
        >
          {section.isSubmitting ? t('userSettings.saving') : t('userSettings.save')}
        </Button>
      </CardContent>
    </Card>
  )
}
