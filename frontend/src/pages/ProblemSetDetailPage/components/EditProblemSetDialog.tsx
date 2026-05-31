import { PencilLine } from 'lucide-react'

import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { ProblemSetContentEditorCard } from './ProblemSetContentEditorCard'
import { ProblemSetDeleteCard } from './ProblemSetDeleteCard'
import { ProblemSetLinkProblemCard } from './ProblemSetLinkProblemCard'
import { useI18n } from '@/system/i18n/use-i18n'

type EditProblemSetDialogProps = {
  open: boolean
  title: string
  description: string
  authorUsername: string
  linkProblemSlug: string
  isSaving: boolean
  isDeleting: boolean
  activeLink: boolean
  contentErrorMessage: string
  contentSuccessMessage: string
  linkErrorMessage: string
  linkSuccessMessage: string
  onOpenChange: (open: boolean) => void
  onTitleChange: (value: string) => void
  onDescriptionChange: (value: string) => void
  onAuthorUsernameChange: (value: string) => void
  onLinkProblemSlugChange: (value: string) => void
  onSaveContent: () => void
  onAttachProblem: () => void
  onDeleteProblemSet: () => Promise<boolean>
}

export function EditProblemSetDialog({
  open,
  title,
  description,
  authorUsername,
  linkProblemSlug,
  isSaving,
  isDeleting,
  activeLink,
  contentErrorMessage,
  contentSuccessMessage,
  linkErrorMessage,
  linkSuccessMessage,
  onOpenChange,
  onTitleChange,
  onDescriptionChange,
  onAuthorUsernameChange,
  onLinkProblemSlugChange,
  onSaveContent,
  onAttachProblem,
  onDeleteProblemSet,
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
            title={title}
            description={description}
            authorUsername={authorUsername}
            isSaving={isSaving}
            contentErrorMessage={contentErrorMessage}
            contentSuccessMessage={contentSuccessMessage}
            onTitleChange={onTitleChange}
            onDescriptionChange={onDescriptionChange}
            onAuthorUsernameChange={onAuthorUsernameChange}
            onSaveContent={onSaveContent}
          />
          <ProblemSetLinkProblemCard
            linkProblemSlug={linkProblemSlug}
            activeLink={activeLink}
            linkErrorMessage={linkErrorMessage}
            linkSuccessMessage={linkSuccessMessage}
            onLinkProblemSlugChange={onLinkProblemSlugChange}
            onAttachProblem={onAttachProblem}
          />
          <ProblemSetDeleteCard isDeleting={isDeleting} onDeleteProblemSet={onDeleteProblemSet} />
        </div>
      </DialogContent>
    </Dialog>
  )
}
