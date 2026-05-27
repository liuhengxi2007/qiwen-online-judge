import { displayNameValue } from '@/objects/user/user-parsers'
import type { useUserGroupDetailPageModel } from '../hooks/use-usergroup-detail-page-model'
import { ConfirmActionDialog } from '@/pages/components/confirm-action-dialog'
import { useI18n } from '@/system/i18n/use-i18n'

type UserGroupDetailPageModel = ReturnType<typeof useUserGroupDetailPageModel>

type UserGroupOwnershipTransferDialogProps = {
  model: UserGroupDetailPageModel
  onOpenChange: (open: boolean) => void
  ownershipTargetMember: NonNullable<UserGroupDetailPageModel['userGroup']>['members'][number] | null
}

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
