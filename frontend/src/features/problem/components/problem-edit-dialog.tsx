import { useDeferredValue } from 'react'
import { PencilLine, Trash2 } from 'lucide-react'

import { Alert, AlertDescription } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs'
import { Textarea } from '@/components/ui/textarea'
import { useNavigate } from 'react-router-dom'
import type { useProblemDetailPageModel } from '@/features/problem/hooks/use-problem-detail-page-model'
import { ConfirmActionDialog } from '@/shared/components/confirm-action-dialog'
import { MarkdownDocument } from '@/shared/components/markdown-document'
import { useI18n } from '@/shared/i18n/use-i18n'

type ProblemDetailPageModel = ReturnType<typeof useProblemDetailPageModel>

type ProblemEditDialogProps = {
  model: ProblemDetailPageModel
  open: boolean
  setOpen: (open: boolean) => void
  statementTab: 'write' | 'preview'
  setStatementTab: (value: 'write' | 'preview') => void
}

export function ProblemEditDialog({
  model,
  open,
  setOpen,
  statementTab,
  setStatementTab,
}: ProblemEditDialogProps) {
  const { t } = useI18n()
  const navigate = useNavigate()
  const deferredStatement = useDeferredValue(model.statement)

  return (
    <Dialog open={open} onOpenChange={setOpen}>
      <DialogContent
        className="max-h-[calc(100vh-2rem)] max-w-4xl overflow-y-auto rounded-[2rem] border-slate-200 bg-white p-0 shadow-[0_28px_90px_rgba(15,23,42,0.22)]"
        onOpenAutoFocus={(event) => {
          event.preventDefault()
        }}
      >
        <DialogHeader className="border-b border-slate-200 px-7 py-6 sm:px-8">
          <DialogTitle className="flex items-center gap-3 text-2xl text-slate-950">
            <span className="flex size-12 items-center justify-center rounded-2xl bg-emerald-100 text-emerald-700">
              <PencilLine className="size-5" />
            </span>
            {t('problem.detail.editDialogTitle')}
          </DialogTitle>
          <DialogDescription className="text-sm leading-7 text-slate-600">
            {t('problem.detail.editDialogDescription')}
          </DialogDescription>
        </DialogHeader>

        <div className="space-y-6 px-7 py-7 sm:px-8">
          <div className="space-y-2">
            <Label htmlFor="problem-title">{t('problem.create.titleLabel')}</Label>
            <Input id="problem-title" value={model.title} onChange={(event) => model.setTitle(event.target.value)} />
          </div>
          <div className="space-y-2">
            <Label htmlFor="problem-statement">{t('problem.create.statement')}</Label>
            <Tabs value={statementTab} onValueChange={(value) => setStatementTab(value as 'write' | 'preview')}>
              <TabsList className="grid w-full grid-cols-2 rounded-2xl bg-slate-100">
                <TabsTrigger value="write" className="rounded-xl">
                  {t('common.write')}
                </TabsTrigger>
                <TabsTrigger value="preview" className="rounded-xl">
                  {t('common.preview')}
                </TabsTrigger>
              </TabsList>
              <TabsContent value="write" className="mt-3">
                <Textarea
                  id="problem-statement"
                  className="min-h-64 !font-mono"
                  value={model.statement}
                  onChange={(event) => model.setStatement(event.target.value)}
                />
              </TabsContent>
              <TabsContent value="preview" className="mt-3">
                <div className="rounded-3xl border border-slate-200 bg-slate-50 px-6 py-6">
                  {deferredStatement.trim() ? (
                    <MarkdownDocument content={deferredStatement} />
                  ) : (
                    <p className="text-sm text-slate-500">{t('common.nothingToPreview')}</p>
                  )}
                </div>
              </TabsContent>
            </Tabs>
            <p className="text-xs text-slate-500">{t('problem.create.markdownHelp')}</p>
          </div>
          <div className="grid gap-5 sm:grid-cols-2">
            <div className="space-y-2">
              <Label htmlFor="problem-time-limit">{t('problem.detail.timeLimit')}</Label>
              <Input
                id="problem-time-limit"
                type="number"
                min={1}
                value={model.timeLimitMs}
                onChange={(event) => {
                  model.setTimeLimitMs(Number(event.target.value))
                }}
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="problem-space-limit">{t('problem.detail.spaceLimit')}</Label>
              <Input
                id="problem-space-limit"
                type="number"
                min={1}
                value={model.spaceLimitMb}
                onChange={(event) => {
                  model.setSpaceLimitMb(Number(event.target.value))
                }}
              />
            </div>
          </div>
          <Button
            type="button"
            className="rounded-2xl bg-slate-950 text-white hover:bg-slate-800"
            disabled={model.isSaving}
            onClick={() => {
              void model.saveContent()
            }}
          >
            {model.isSaving ? t('problem.detail.savingContent') : t('problem.detail.saveContent')}
          </Button>
          {model.contentErrorMessage ? (
            <Alert variant="destructive" className="rounded-2xl border-rose-200 bg-rose-50/95">
              <AlertDescription className="text-rose-700">{model.contentErrorMessage}</AlertDescription>
            </Alert>
          ) : null}
          {model.contentSuccessMessage ? (
            <Alert className="rounded-2xl border-emerald-200 bg-emerald-50/95">
              <AlertDescription className="text-emerald-700">{model.contentSuccessMessage}</AlertDescription>
            </Alert>
          ) : null}

          <div className="rounded-[1.75rem] border border-rose-200 bg-rose-50/60 p-6">
            <div className="flex items-center gap-3">
              <div className="flex size-12 items-center justify-center rounded-2xl bg-rose-100 text-rose-700">
                <Trash2 className="size-5" />
              </div>
              <div>
                <h2 className="text-xl font-semibold text-rose-950">{t('problem.detail.deleteTitle')}</h2>
                <p className="text-sm text-rose-900/80">{t('problem.detail.deleteDescription')}</p>
              </div>
            </div>
            <div className="mt-5">
              <ConfirmActionDialog
                title={t('problem.detail.deleteConfirmTitle')}
                description={t('problem.detail.deleteConfirmDescription')}
                confirmLabel={model.isDeleting ? t('problem.detail.deletingAction') : t('problem.detail.deleteAction')}
                destructive
                onConfirm={() => {
                  void model.deleteCurrentProblem().then((deleted) => {
                    if (deleted) {
                      void navigate('/problems')
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
                    {model.isDeleting ? t('problem.detail.deletingAction') : t('problem.detail.deleteAction')}
                  </Button>
                }
              />
            </div>
          </div>
        </div>
      </DialogContent>
    </Dialog>
  )
}
