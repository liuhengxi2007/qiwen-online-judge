import { useState } from 'react'

import { Alert, AlertDescription } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { MarkdownEditorTabs } from '@/pages/components/MarkdownEditorTabs'
import { useI18n } from '@/system/i18n/use-i18n'

type ProblemSetContentEditorCardProps = {
  title: string
  description: string
  authorUsername: string
  isSaving: boolean
  contentErrorMessage: string
  contentSuccessMessage: string
  onTitleChange: (value: string) => void
  onDescriptionChange: (value: string) => void
  onAuthorUsernameChange: (value: string) => void
  onSaveContent: () => void
}

export function ProblemSetContentEditorCard({
  title,
  description,
  authorUsername,
  isSaving,
  contentErrorMessage,
  contentSuccessMessage,
  onTitleChange,
  onDescriptionChange,
  onAuthorUsernameChange,
  onSaveContent,
}: ProblemSetContentEditorCardProps) {
  const { t } = useI18n()
  const [descriptionTab, setDescriptionTab] = useState<'write' | 'preview'>('write')

  return (
    <>
      <div className="space-y-2">
        <Label htmlFor="problem-set-title">{t('problemSet.create.titleLabel')}</Label>
        <Input
          id="problem-set-title"
          value={title}
          onChange={(event) => {
            onTitleChange(event.target.value)
          }}
        />
      </div>
      <div className="space-y-2">
        <Label htmlFor="problem-set-author-username">{t('problemSet.detail.authorUsername')}</Label>
        <Input
          id="problem-set-author-username"
          value={authorUsername}
          onChange={(event) => {
            onAuthorUsernameChange(event.target.value)
          }}
        />
      </div>
      <div className="space-y-2">
        <Label htmlFor="problem-set-description">{t('problemSet.create.descriptionLabel')}</Label>
        <MarkdownEditorTabs
          textareaId="problem-set-description"
          value={description}
          tab={descriptionTab}
          onTabChange={setDescriptionTab}
          onValueChange={onDescriptionChange}
          textareaClassName="min-h-48 font-mono"
        />
        <p className="text-xs text-slate-500">{t('problem.create.markdownHelp')}</p>
      </div>
      <Button
        type="button"
        className="rounded-2xl bg-slate-950 text-white hover:bg-slate-800"
        disabled={isSaving}
        onClick={onSaveContent}
      >
        {isSaving ? t('problemSet.detail.savingContent') : t('problemSet.detail.saveContent')}
      </Button>
      {contentErrorMessage ? (
        <Alert variant="destructive" className="rounded-2xl border-rose-200 bg-rose-50/95">
          <AlertDescription className="text-rose-700">{contentErrorMessage}</AlertDescription>
        </Alert>
      ) : null}
      {contentSuccessMessage ? (
        <Alert className="rounded-2xl border-emerald-200 bg-emerald-50/95">
          <AlertDescription className="text-emerald-700">{contentSuccessMessage}</AlertDescription>
        </Alert>
      ) : null}
    </>
  )
}
