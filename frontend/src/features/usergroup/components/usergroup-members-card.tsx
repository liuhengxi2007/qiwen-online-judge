import { Trash2, Users } from 'lucide-react'

import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { RadioGroup, RadioGroupItem } from '@/components/ui/radio-group'
import { displayNameValue, usernameValue } from '@/features/auth/domain/auth'
import type { useUserGroupDetailPageModel } from '@/features/usergroup/hooks/use-usergroup-detail-page-model'
import { ConfirmActionDialog } from '@/shared/components/confirm-action-dialog'
import { useI18n } from '@/shared/i18n/i18n'

type UserGroupDetailPageModel = ReturnType<typeof useUserGroupDetailPageModel>

type UserGroupMembersCardProps = {
  model: UserGroupDetailPageModel
  setOwnershipTargetUsername: (username: string | null) => void
}

export function UserGroupMembersCard({ model, setOwnershipTargetUsername }: UserGroupMembersCardProps) {
  const { t } = useI18n()

  if (!model.userGroup) {
    return null
  }

  return (
    <Card className="border-slate-200 bg-white shadow-[0_24px_60px_rgba(15,23,42,0.08)]">
      <CardHeader>
        <div className="flex items-center gap-3">
          <div className="flex size-12 items-center justify-center rounded-2xl bg-sky-100 text-sky-700">
            <Users className="size-5" />
          </div>
          <div>
            <CardTitle className="text-xl text-slate-950">{t('userGroup.detail.membersTitle')}</CardTitle>
            <CardDescription>{t('userGroup.detail.membersDescription')}</CardDescription>
          </div>
        </div>
      </CardHeader>
      <CardContent className="space-y-4">
        {model.userGroup.members.map((member) => (
          <div key={member.username} className="rounded-2xl border border-slate-200 bg-slate-50 p-4">
            <div className="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
              <div>
                <p className="text-sm font-medium text-slate-900">{displayNameValue(member.displayName)}</p>
                <p className="font-mono text-xs text-slate-500">{usernameValue(member.username)}</p>
              </div>
              <div className="flex flex-col items-stretch gap-3 sm:items-end">
                <RadioGroup
                  value={member.role}
                  disabled={
                    !model.canManageMemberRoles ||
                    member.role === 'owner' ||
                    model.activeUpdatingUsername === member.username ||
                    model.activeRemovingUsername === member.username
                  }
                  onValueChange={(value) => {
                    if (value !== 'owner' && value !== 'manager' && value !== 'member') {
                      return
                    }

                    if (value === member.role) {
                      return
                    }

                    if (value === 'owner') {
                      setOwnershipTargetUsername(member.username)
                      return
                    }

                    void model.updateMemberRole(member.username, value)
                  }}
                  className="grid gap-2 sm:grid-cols-3"
                >
                  <label className="flex items-center gap-2 rounded-2xl border border-slate-200 bg-white px-3 py-2 text-xs text-slate-600">
                    <RadioGroupItem
                      value="owner"
                      disabled={
                        !model.canManageMemberRoles ||
                        member.role === 'owner' ||
                        model.activeUpdatingUsername === member.username ||
                        model.activeRemovingUsername === member.username
                      }
                    />
                    <span>{t('userGroup.detail.role.owner')}</span>
                  </label>
                  <label className="flex items-center gap-2 rounded-2xl border border-slate-200 bg-white px-3 py-2 text-xs text-slate-600">
                    <RadioGroupItem
                      value="manager"
                      disabled={
                        !model.canManageMemberRoles ||
                        member.role === 'owner' ||
                        model.activeUpdatingUsername === member.username ||
                        model.activeRemovingUsername === member.username
                      }
                    />
                    <span>{t('userGroup.detail.role.manager')}</span>
                  </label>
                  <label className="flex items-center gap-2 rounded-2xl border border-slate-200 bg-white px-3 py-2 text-xs text-slate-600">
                    <RadioGroupItem
                      value="member"
                      disabled={
                        !model.canManageMemberRoles ||
                        member.role === 'owner' ||
                        model.activeUpdatingUsername === member.username ||
                        model.activeRemovingUsername === member.username
                      }
                    />
                    <span>{t('userGroup.detail.role.member')}</span>
                  </label>
                </RadioGroup>

                {member.role !== 'owner' ? (
                  <ConfirmActionDialog
                    title={t('userGroup.detail.removeMemberTitle')}
                    description={t('userGroup.detail.removeMemberDescription', { username: usernameValue(member.username) })}
                    confirmLabel={t('userGroup.detail.removeMemberAction')}
                    destructive
                    onConfirm={() => {
                      void model.removeMember(member.username)
                    }}
                    trigger={
                      <Button
                        type="button"
                        variant="outline"
                        size="sm"
                        className="size-8 rounded-full border-rose-300 bg-white p-0 text-rose-700 hover:bg-rose-50 hover:text-rose-800"
                        aria-label={`Remove ${usernameValue(member.username)} from the group`}
                        disabled={
                          !model.canRemoveMember(member.username, member.role) ||
                          model.activeUpdatingUsername !== null ||
                          model.activeRemovingUsername !== null
                        }
                      >
                        <Trash2 className="size-4" />
                      </Button>
                    }
                  />
                ) : null}
              </div>
            </div>
          </div>
        ))}
      </CardContent>
    </Card>
  )
}
