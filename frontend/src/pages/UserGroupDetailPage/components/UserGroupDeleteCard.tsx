import { useNavigate } from 'react-router-dom'
import { Trash2 } from 'lucide-react'

import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import type { useUserGroupDetailPageModel } from '../hooks/useUserGroupDetailPageModel'
import { ConfirmActionDialog } from '@/pages/components/ConfirmActionDialog'
import { useI18n } from '@/system/i18n/use-i18n'

type UserGroupDetailPageModel = ReturnType<typeof useUserGroupDetailPageModel>

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
              variant="outline"
              className="rounded-2xl border-rose-300 bg-white text-rose-700 hover:bg-rose-100 hover:text-rose-800"
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
