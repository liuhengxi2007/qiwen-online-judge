import { useNavigate } from 'react-router-dom'
import { Trash2 } from 'lucide-react'

import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import type { useUserGroupDetailPageModel } from '../hooks/useUserGroupDetailPageModel'
import { ConfirmActionDialog } from '@/pages/components/ConfirmActionDialog'
import { useI18n } from '@/system/i18n/use-i18n'

/**
 * 用户组详情页模型类型别名，供删除卡片读取删除权限和动作状态。
 */
type UserGroupDetailPageModel = ReturnType<typeof useUserGroupDetailPageModel>

/**
 * 用户组删除卡片，仅在当前访问者拥有删除权限时展示确认操作。
 * 删除成功后由组件导航回用户组列表。
 */
export function UserGroupDeleteCard({ model }: { model: UserGroupDetailPageModel }) {
  const { t } = useI18n()
  const navigate = useNavigate()

  if (!model.canDelete) {
    return null
  }

  return (
    <Card className="border-rose-200 bg-rose-50/60 shadow-[0_24px_60px_rgba(15,23,42,0.08)]">
      <CardHeader>
        <div className="flex items-center gap-3">
          <div className="flex size-12 items-center justify-center rounded-2xl bg-rose-100 text-rose-700">
            <Trash2 className="size-5" />
          </div>
          <div>
            <CardTitle className="text-xl text-rose-950">{t('userGroup.detail.deleteTitle')}</CardTitle>
            <CardDescription>{t('userGroup.detail.deleteDescription')}</CardDescription>
          </div>
        </div>
      </CardHeader>
      <CardContent>
        <ConfirmActionDialog
          title={t('userGroup.detail.deleteConfirmTitle')}
          description={t('userGroup.detail.deleteConfirmDescription')}
          confirmLabel={model.isDeleting ? t('problemSet.detail.deletingAction') : t('userGroup.detail.deleteAction')}
          destructive
          onConfirm={() => {
            void model.deleteCurrentUserGroup().then((deleted) => {
              if (deleted) {
                void navigate('/user-groups')
              }
            })
          }}
          trigger={
            <Button
              type="button"
              variant="destructiveOutline"
              disabled={model.isDeleting}
            >
              {model.isDeleting ? t('problemSet.detail.deletingAction') : t('userGroup.detail.deleteAction')}
            </Button>
          }
        />
      </CardContent>
    </Card>
  )
}
