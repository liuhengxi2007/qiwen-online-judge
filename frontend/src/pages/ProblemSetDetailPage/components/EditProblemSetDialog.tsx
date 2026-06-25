import { PencilLine } from 'lucide-react'

import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { ProblemSetContentEditorCard } from './ProblemSetContentEditorCard'
import { ProblemSetDeleteCard } from './ProblemSetDeleteCard'
import { ProblemSetLinkProblemCard } from './ProblemSetLinkProblemCard'
import { useI18n } from '@/system/i18n/use-i18n'
import type { ProblemSetDetailPageModel } from '../hooks/useProblemSetDetailPageModel'

/**
 * 编辑题单对话框属性，包含打开状态和题单详情页模型。
 */
type EditProblemSetDialogProps = {
  open: boolean
  onOpenChange: (open: boolean) => void
  model: ProblemSetDetailPageModel
}

/**
 * 编辑题单对话框，提供标题和描述编辑表单。
 */
export function EditProblemSetDialog({
  open,
  onOpenChange,
  model,
}: EditProblemSetDialogProps) {
  const { t } = useI18n()

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-h-[calc(100vh-2rem)] max-w-4xl overflow-y-auto rounded-[2rem] border-slate-200 bg-white p-0 shadow-[0_28px_90px_rgba(15,23,42,0.22)]">
        <DialogHeader className="border-b border-slate-200 px-7 py-6 sm:px-8">
          <DialogTitle className="flex items-center gap-3 text-2xl text-slate-950">
            <span className="flex size-12 items-center justify-center rounded-2xl bg-amber-100 text-amber-700">
              <PencilLine className="size-5" />
            </span>
            {t('problemSet.detail.editDialogTitle')}
          </DialogTitle>
          <DialogDescription className="text-sm leading-7 text-slate-600">
            {t('problemSet.detail.editDialogDescription')}
          </DialogDescription>
        </DialogHeader>

        <div className="space-y-6 px-7 py-7 sm:px-8">
          <ProblemSetContentEditorCard
            draft={{
              title: model.title,
              description: model.description,
              authorUsername: model.authorUsername,
            }}
            status={{
              isSaving: model.isSaving,
              contentErrorMessage: model.contentErrorMessage,
              contentSuccessMessage: model.contentSuccessMessage,
            }}
            actions={{
              onTitleChange: model.setTitle,
              onDescriptionChange: model.setDescription,
              onAuthorUsernameChange: model.setAuthorUsername,
              onSaveContent: () => {
                void model.saveContent()
              },
            }}
          />
          <ProblemSetLinkProblemCard
            draft={{ linkProblemSlug: model.linkProblemSlug }}
            status={{
              activeLink: model.activeLink,
              linkErrorMessage: model.linkErrorMessage,
              linkSuccessMessage: model.linkSuccessMessage,
            }}
            actions={{
              onLinkProblemSlugChange: model.setLinkProblemSlug,
              onAttachProblem: () => {
                void model.attachProblem()
              },
            }}
          />
          <ProblemSetDeleteCard isDeleting={model.isDeleting} onDeleteProblemSet={model.deleteCurrentProblemSet} />
        </div>
      </DialogContent>
    </Dialog>
  )
}
