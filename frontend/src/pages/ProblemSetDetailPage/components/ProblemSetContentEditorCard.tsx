import { useDeferredValue, useState } from 'react'

import { Alert, AlertDescription } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs'
import { Textarea } from '@/components/ui/textarea'
import { MarkdownDocument } from '@/pages/components/MarkdownDocument'
import { useI18n } from '@/system/i18n/use-i18n'

type ProblemSetContentEditorCardProps = {
  title: string
  description: string
  isSaving: boolean
  contentErrorMessage: string
  contentSuccessMessage: string
  onTitleChange: (value: string) => void
  onDescriptionChange: (value: string) => void
  onSaveContent: () => void
}

export function ProblemSetContentEditorCard({
  title,
  description,
  isSaving,
  contentErrorMessage,
  contentSuccessMessage,
  onTitleChange,
  onDescriptionChange,
  onSaveContent,
}: ProblemSetContentEditorCardProps) {
  const { t } = useI18n()
  const [descriptionTab, setDescriptionTab] = useState<'write' | 'preview'>('write')
  const deferredDescription = useDeferredValue(description)

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
        <Label htmlFor="problem-set-description">{t('problemSet.create.descriptionLabel')}</Label>
        <Tabs value={descriptionTab} onValueChange={(value) => setDescriptionTab(value as 'write' | 'preview')}>
          <TabsList className="grid w-full grid-cols-2 rounded-2xl bg-slate-100">
            <TabsTrigger value="write" className="rounded-xl">{t('common.write')}</TabsTrigger>
            <TabsTrigger value="preview" className="rounded-xl">{t('common.preview')}</TabsTrigger>
          </TabsList>
          <TabsContent value="write" className="mt-3">
            <Textarea
              id="problem-set-description"
              value={description}
              className="min-h-48 !font-mono"
              onChange={(event) => {
                onDescriptionChange(event.target.value)
              }}
            />
          </TabsContent>
          <TabsContent value="preview" className="mt-3">
            <div className="rounded-3xl border border-slate-200 bg-slate-50 px-6 py-6">
              {deferredDescription.trim() ? (
                <MarkdownDocument content={deferredDescription} />
              ) : (
                <p className="text-sm text-slate-500">{t('common.nothingToPreview')}</p>
              )}
            </div>
          </TabsContent>
        </Tabs>
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
