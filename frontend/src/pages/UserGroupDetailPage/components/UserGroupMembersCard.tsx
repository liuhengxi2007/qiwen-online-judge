import { Trash2, Users } from 'lucide-react'

import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { RadioGroup, RadioGroupItem } from '@/components/ui/radio-group'
import { displayNameValue } from '@/objects/user/DisplayName'
import type { useUserGroupDetailPageModel } from '../hooks/useUserGroupDetailPageModel'
import { ConfirmActionDialog } from '@/pages/components/ConfirmActionDialog'
import { UserProfileLink } from '@/pages/components/UserProfileLink'
import { useI18n } from '@/system/i18n/use-i18n'

/**
 * 用户组详情页模型类型别名，供成员列表读取成员、权限和成员操作回调。
 */
type UserGroupDetailPageModel = ReturnType<typeof useUserGroupDetailPageModel>

/**
 * 成员列表卡片属性，包含页面模型和设置所有权转移目标的回调。
 */
type UserGroupMembersCardProps = {
  model: UserGroupDetailPageModel
  setOwnershipTargetUsername: (username: string | null) => void
}

/**
 * 用户组成员卡片，展示成员资料、角色单选控件和移除按钮。
 * 角色变更和移除是否可用完全依赖页面模型计算出的权限与进行中状态。
 */
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
            {(() => {
              const roleControlsDisabled =
                !model.canManageMemberRoles ||
                member.role === 'owner' ||
                model.activeUpdatingUsername === member.username ||
                model.activeRemovingUsername === member.username
              const removeDisabled =
                !model.canRemoveMember(member.username, member.role) ||
                model.activeUpdatingUsername !== null ||
                model.activeRemovingUsername !== null

              return (
                <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
                  <div>
                    <UserProfileLink stacked user={member} />
                  </div>
                  <div className="flex flex-col gap-3 sm:flex-row sm:items-center">
                    <RadioGroup
                      value={member.role}
                      disabled={roleControlsDisabled}
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
                        <RadioGroupItem value="owner" disabled={roleControlsDisabled} />
                        <span>{t('userGroup.detail.role.owner')}</span>
                      </label>
                      <label className="flex items-center gap-2 rounded-2xl border border-slate-200 bg-white px-3 py-2 text-xs text-slate-600">
                        <RadioGroupItem value="manager" disabled={roleControlsDisabled} />
                        <span>{t('userGroup.detail.role.manager')}</span>
                      </label>
                      <label className="flex items-center gap-2 rounded-2xl border border-slate-200 bg-white px-3 py-2 text-xs text-slate-600">
                        <RadioGroupItem value="member" disabled={roleControlsDisabled} />
                        <span>{t('userGroup.detail.role.member')}</span>
                      </label>
                    </RadioGroup>

                    {removeDisabled ? (
                      <Button
                        type="button"
                        variant="destructiveOutline"
                        size="sm"
                        className="size-8 rounded-full p-0"
                        aria-label={`Remove ${displayNameValue(member.displayName)} from the group`}
                        disabled
                      >
                        <Trash2 className="size-4" />
                      </Button>
                    ) : (
                      <ConfirmActionDialog
                        title={t('userGroup.detail.removeMemberTitle')}
                        description={t('userGroup.detail.removeMemberDescription', { username: displayNameValue(member.displayName) })}
                        confirmLabel={t('userGroup.detail.removeMemberAction')}
                        destructive
                        onConfirm={() => {
                          void model.removeMember(member.username)
                        }}
                        trigger={
                          <Button
                            type="button"
                            variant="destructiveOutline"
                            size="sm"
                            className="size-8 rounded-full p-0"
                            aria-label={`Remove ${displayNameValue(member.displayName)} from the group`}
                          >
                            <Trash2 className="size-4" />
                          </Button>
                        }
                      />
                    )}
                  </div>
                </div>
              )
            })()}
          </div>
        ))}
      </CardContent>
    </Card>
  )
}
