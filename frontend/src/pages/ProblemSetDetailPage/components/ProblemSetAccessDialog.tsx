import { ShieldCheck } from 'lucide-react'

import { Alert, AlertDescription } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { ResourceAccessEditor } from '@/pages/components/ResourceAccessEditor'
import { resourceAccessSummary } from '@/pages/objects/ResourceAccessDisplay'
import { useI18n } from '@/system/i18n/use-i18n'
import type { ProblemSetDetailPageModel } from '../hooks/useProblemSetDetailPageModel'

/**
 * 题单访问控制对话框属性，包含打开状态和题单详情页模型。
 */
type ProblemSetAccessDialogProps = {
  open: boolean
  onOpenChange: (open: boolean) => void
  model: ProblemSetDetailPageModel
}

/**
 * 题单访问控制对话框，渲染资源访问编辑器并保存访问策略。
 */
export function ProblemSetAccessDialog({
  open,
  onOpenChange,
  model,
}: ProblemSetAccessDialogProps) {
  const { t } = useI18n()
  const {
    accessPolicy,
    problemSet,
    grantedUsersInput,
    grantedGroupsInput,
    isSaving,
    accessErrorMessage,
    accessSuccessMessage,
    setBaseAccess,
    setGrantedUsersInput,
    setGrantedGroupsInput,
    saveAccess,
  } = model
  const summaryPolicy = problemSet?.accessPolicy ?? accessPolicy

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-h-[calc(100vh-2rem)] max-w-3xl overflow-y-auto rounded-[2rem] border-slate-200 bg-white p-0 shadow-[0_28px_90px_rgba(15,23,42,0.22)]">
        <DialogHeader className="border-b border-slate-200 px-7 py-6 sm:px-8">
          <DialogTitle className="flex items-center gap-3 text-2xl text-slate-950">
            <span className="flex size-12 items-center justify-center rounded-2xl bg-teal-100 text-teal-700">
              <ShieldCheck className="size-5" />
            </span>
            {t('problemSet.detail.accessDialogTitle')}
          </DialogTitle>
          <DialogDescription className="text-sm leading-7 text-slate-600">{t('problemSet.detail.accessDialogDescription')}</DialogDescription>
        </DialogHeader>

        <div className="space-y-5 px-7 py-7 sm:px-8">
          <p className="text-sm text-slate-600">{resourceAccessSummary(summaryPolicy, t)}</p>
          <ResourceAccessEditor
            accessPolicy={accessPolicy}
            grantedUsersInput={grantedUsersInput}
            grantedGroupsInput={grantedGroupsInput}
            onBaseAccessChange={setBaseAccess}
            onGrantedUsersInputChange={setGrantedUsersInput}
            onGrantedGroupsInputChange={setGrantedGroupsInput}
          />
          <Button type="button" disabled={isSaving} onClick={() => {
            void saveAccess()
          }}>
            {isSaving ? t('problemSet.detail.savingAccess') : t('problemSet.detail.saveAccess')}
          </Button>
          {accessErrorMessage ? (
            <Alert variant="destructive">
              <AlertDescription>{accessErrorMessage}</AlertDescription>
            </Alert>
          ) : null}
          {accessSuccessMessage ? (
            <Alert variant="success">
              <AlertDescription>{accessSuccessMessage}</AlertDescription>
            </Alert>
          ) : null}
        </div>
      </DialogContent>
    </Dialog>
  )
}
