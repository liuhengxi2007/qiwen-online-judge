import { displayNameValue } from '@/objects/user/DisplayName'
import type { useUserGroupDetailPageModel } from '../hooks/useUserGroupDetailPageModel'
import { ConfirmActionDialog } from '@/pages/components/ConfirmActionDialog'
import { useI18n } from '@/system/i18n/use-i18n'

/**
 * 用户组详情页模型类型别名，供所有权转移对话框调用角色更新动作。
 */
type UserGroupDetailPageModel = ReturnType<typeof useUserGroupDetailPageModel>

/**
 * 所有权转移确认框属性，包含目标成员、打开状态回调和页面模型。
 */
type UserGroupOwnershipTransferDialogProps = {
  model: UserGroupDetailPageModel
  onOpenChange: (open: boolean) => void
  ownershipTargetMember: NonNullable<UserGroupDetailPageModel['userGroup']>['members'][number] | null
}

/**
 * 用户组所有权转移确认框，将目标成员角色更新为 owner 并在完成后关闭弹窗。
 */
export function UserGroupOwnershipTransferDialog({
  model,
  onOpenChange,
  ownershipTargetMember,
}: UserGroupOwnershipTransferDialogProps) {
  const { t } = useI18n()

  return (
    <ConfirmActionDialog
      open={ownershipTargetMember !== null}
      onOpenChange={onOpenChange}
      title={t('userGroup.detail.transferOwnershipTitle')}
      description={
        ownershipTargetMember
          ? t('userGroup.detail.transferOwnershipDescription', {
              username: displayNameValue(ownershipTargetMember.displayName),
            })
          : ''
      }
      confirmLabel={t('userGroup.detail.transferOwnershipAction')}
      onConfirm={() => {
        if (!ownershipTargetMember) {
          return
        }

        void model.updateMemberRole(ownershipTargetMember.username, 'owner').then(() => {
          onOpenChange(false)
        })
      }}
    />
  )
}
