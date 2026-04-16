import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { usernameValue } from '@/features/auth/domain/auth'
import { userGroupDescriptionValue, userGroupNameValue, userGroupSlugValue } from '@/features/usergroup/domain/usergroup'
import type { useUserGroupDetailPageModel } from '@/features/usergroup/hooks/use-usergroup-detail-page-model'
import { useI18n } from '@/shared/i18n/i18n'

type UserGroupDetailPageModel = ReturnType<typeof useUserGroupDetailPageModel>

export function UserGroupSummaryCard({ model }: { model: UserGroupDetailPageModel }) {
  const { t } = useI18n()

  if (!model.userGroup) {
    return null
  }

  return (
    <Card className="border-slate-200 bg-white shadow-[0_24px_60px_rgba(15,23,42,0.08)]">
      <CardHeader>
        <CardTitle className="text-2xl text-slate-950">{userGroupNameValue(model.userGroup.name)}</CardTitle>
        <CardDescription className="mt-2 font-mono text-sm text-slate-500">
          {userGroupSlugValue(model.userGroup.slug)}
        </CardDescription>
      </CardHeader>
      <CardContent className="space-y-4">
        <p className="text-sm leading-7 text-slate-600">
          {userGroupDescriptionValue(model.userGroup.description) || t('common.noDescription')}
        </p>
        <p className="text-xs uppercase tracking-[0.18em] text-slate-400">
          {t('userGroup.detail.owner', { username: usernameValue(model.userGroup.ownerUsername) })}
        </p>
      </CardContent>
    </Card>
  )
}
