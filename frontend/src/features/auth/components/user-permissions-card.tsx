import { ShieldCheck } from 'lucide-react'

import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Checkbox } from '@/components/ui/checkbox'
import type { SessionResponse } from '@/features/auth/domain/auth'
import { useI18n } from '@/shared/i18n/i18n'

type UserPermissionsCardProps = {
  user: SessionResponse | null
  title: string
  description: string
}

export function UserPermissionsCard({
  user,
  title,
  description,
}: UserPermissionsCardProps) {
  const { t } = useI18n()

  return (
    <Card className="border-slate-200 bg-white shadow-[0_24px_60px_rgba(15,23,42,0.08)]">
      <CardHeader>
        <div className="flex items-center gap-3">
          <div className="flex size-12 items-center justify-center rounded-2xl bg-violet-100 text-violet-700">
            <ShieldCheck className="size-5" />
          </div>
          <div>
            <CardTitle className="text-xl text-slate-950">{title}</CardTitle>
            <CardDescription>{description}</CardDescription>
          </div>
        </div>
      </CardHeader>
      <CardContent className="space-y-4">
        <div className="flex items-center justify-between rounded-2xl bg-slate-50 px-5 py-4">
          <div>
            <p className="font-medium text-slate-900">{t('siteManage.siteManager')}</p>
            <p className="text-sm text-slate-500">{t('userSettings.siteManagerDescription')}</p>
          </div>
          <Checkbox checked={user?.siteManager ?? false} disabled aria-label="Site manager permission" />
        </div>
        <div className="flex items-center justify-between rounded-2xl bg-slate-50 px-5 py-4">
          <div>
            <p className="font-medium text-slate-900">{t('siteManage.problemManager')}</p>
            <p className="text-sm text-slate-500">{t('userSettings.problemManagerDescription')}</p>
          </div>
          <Checkbox checked={user?.problemManager ?? false} disabled aria-label="Problem manager permission" />
        </div>
      </CardContent>
    </Card>
  )
}
