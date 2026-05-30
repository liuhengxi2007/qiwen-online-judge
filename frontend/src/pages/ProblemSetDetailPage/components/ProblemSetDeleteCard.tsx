import { useNavigate } from 'react-router-dom'
import { Trash2 } from 'lucide-react'

import { Button } from '@/components/ui/button'
import { ConfirmActionDialog } from '@/pages/components/ConfirmActionDialog'
import { useI18n } from '@/system/i18n/use-i18n'

type ProblemSetDeleteCardProps = {
  isDeleting: boolean
  onDeleteProblemSet: () => Promise<boolean>
}

export function ProblemSetDeleteCard({ isDeleting, onDeleteProblemSet }: ProblemSetDeleteCardProps) {
  const { t } = useI18n()
  const navigate = useNavigate()

  return (
    <div className="rounded-[1.75rem] border border-rose-200 bg-rose-50/60 p-6">
      <div className="flex items-center gap-3">
        <div className="flex size-12 items-center justify-center rounded-2xl bg-rose-100 text-rose-700">
          <Trash2 className="size-5" />
        </div>
        <div>
          <h2 className="text-xl font-semibold text-rose-950">{t('problemSet.detail.deleteTitle')}</h2>
          <p className="text-sm text-rose-900/80">{t('problemSet.detail.deleteDescription')}</p>
        </div>
      </div>
      <div className="mt-5">
        <ConfirmActionDialog
          title={t('problemSet.detail.deleteConfirmTitle')}
          description={t('problemSet.detail.deleteConfirmDescription')}
          confirmLabel={isDeleting ? t('problemSet.detail.deletingAction') : t('problemSet.detail.deleteAction')}
          destructive
          onConfirm={() => {
            void onDeleteProblemSet().then((deleted) => {
              if (deleted) {
                void navigate('/problem-sets')
              }
            })
          }}
          trigger={
            <Button
              type="button"
              variant="outline"
              className="rounded-2xl border-rose-300 bg-white text-rose-700 hover:bg-rose-100 hover:text-rose-800"
              disabled={isDeleting}
            >
              {isDeleting ? t('problemSet.detail.deletingAction') : t('problemSet.detail.deleteAction')}
            </Button>
          }
        />
      </div>
    </div>
  )
}
