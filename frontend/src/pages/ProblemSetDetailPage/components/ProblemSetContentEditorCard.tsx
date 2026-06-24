import { useState } from 'react'

import { Alert, AlertDescription } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { MarkdownEditorTabs } from '@/pages/components/MarkdownEditorTabs'
import { useI18n } from '@/system/i18n/use-i18n'

/**
 * 题单内容编辑卡片属性，包含编辑模型和保存回调。
 */
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

/**
 * 题单内容编辑卡片，渲染标题、描述和保存操作。
 */
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
        disabled={isSaving}
        onClick={onSaveContent}
      >
        {isSaving ? t('problemSet.detail.savingContent') : t('problemSet.detail.saveContent')}
      </Button>
      {contentErrorMessage ? (
        <Alert variant="destructive">
          <AlertDescription>{contentErrorMessage}</AlertDescription>
        </Alert>
      ) : null}
      {contentSuccessMessage ? (
        <Alert variant="success">
          <AlertDescription>{contentSuccessMessage}</AlertDescription>
        </Alert>
      ) : null}
    </>
  )
}
