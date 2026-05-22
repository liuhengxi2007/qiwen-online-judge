import { UserRoundPen } from 'lucide-react'

import { Alert, AlertDescription } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import type { SessionResponse } from '@/features/auth/domain/auth'
import type { UserSettingsSectionState } from '@/features/user/domain/user-settings-state'
import { useI18n } from '@/shared/i18n/use-i18n'

type UserSettingsProfileCardProps = {
  displayedUser: SessionResponse | null
  displayName: string
  section: UserSettingsSectionState
  setDisplayName: (value: string) => void
  submit: () => void
}

export function UserSettingsProfileCard({
  displayedUser,
  displayName,
  section,
  setDisplayName,
  submit,
}: UserSettingsProfileCardProps) {
  const { t } = useI18n()

  return (
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
            disabled={section.isSubmitting || !displayedUser}
            className="rounded-2xl bg-violet-300 text-violet-950 hover:bg-violet-400"
            onClick={submit}
          >
            {section.isSubmitting ? t('userSettings.saving') : t('userSettings.save')}
          </Button>
        </div>
      </CardContent>
    </Card>
  )
}
