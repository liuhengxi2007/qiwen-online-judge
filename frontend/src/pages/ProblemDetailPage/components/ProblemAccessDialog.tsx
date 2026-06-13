import { ShieldCheck } from 'lucide-react'

import { Alert, AlertDescription } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { Label } from '@/components/ui/label'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import type { useProblemDetailPageModel } from '../hooks/useProblemDetailPageModel'
import { ResourceAccessEditor } from '@/pages/components/ResourceAccessEditor'
import { resourceAccessSummary } from '@/pages/objects/ResourceAccessDisplay'
import { useI18n } from '@/system/i18n/use-i18n'

/**
 * 题目详情页模型类型别名，供访问控制对话框读取编辑状态。
 */
type ProblemDetailPageModel = ReturnType<typeof useProblemDetailPageModel>

/**
 * 题目访问控制对话框属性，包含详情页模型和保存回调。
 */
type ProblemAccessDialogProps = {
  model: ProblemDetailPageModel
  open: boolean
  otherUserSubmissionAccessOptions: ReadonlyArray<{ value: 'none' | 'summary' | 'detail'; label: string; description: string }>
  setOpen: (open: boolean) => void
}

/**
 * 题目访问控制对话框，渲染资源访问编辑器并保存访问策略。
 */
export function ProblemAccessDialog({
  model,
  open,
  otherUserSubmissionAccessOptions,
  setOpen,
}: ProblemAccessDialogProps) {
  const { t } = useI18n()

  return (
    <Dialog open={open} onOpenChange={setOpen}>
      <DialogContent
        className="max-h-[calc(100vh-2rem)] max-w-3xl overflow-y-auto rounded-[2rem] border-slate-200 bg-white p-0 shadow-[0_28px_90px_rgba(15,23,42,0.22)]"
        onOpenAutoFocus={(event) => {
          event.preventDefault()
        }}
      >
        <DialogHeader className="border-b border-slate-200 px-7 py-6 sm:px-8">
          <DialogTitle className="flex items-center gap-3 text-2xl text-slate-950">
            <span className="flex size-12 items-center justify-center rounded-2xl bg-emerald-100 text-emerald-700">
              <ShieldCheck className="size-5" />
            </span>
            {t('problem.detail.accessDialogTitle')}
          </DialogTitle>
          <DialogDescription className="text-sm leading-7 text-slate-600">
            {t('problem.detail.accessDialogDescription')}
          </DialogDescription>
        </DialogHeader>

        <div className="space-y-5 px-7 py-7 sm:px-8">
          <p className="text-sm text-slate-600">{resourceAccessSummary(model.problem?.accessPolicy ?? model.accessPolicy, t)}</p>
          <ResourceAccessEditor
            accessPolicy={model.accessPolicy}
            grantedUsersInput={model.grantedUsersInput}
            grantedGroupsInput={model.grantedGroupsInput}
            grantedManagerUsersInput={model.managerUsersInput}
            grantedManagerGroupsInput={model.managerGroupsInput}
            onBaseAccessChange={model.setBaseAccess}
            onGrantedUsersInputChange={model.setGrantedUsersInput}
            onGrantedGroupsInputChange={model.setGrantedGroupsInput}
            onGrantedManagerUsersInputChange={model.setManagerUsersInput}
            onGrantedManagerGroupsInputChange={model.setManagerGroupsInput}
          />
          <div className="space-y-2">
            <Label htmlFor="problem-other-user-submission-access">{t('problem.create.otherUserSubmissionAccess')}</Label>
            <Select value={model.otherUserSubmissionAccess} onValueChange={model.setOtherUserSubmissionAccess}>
              <SelectTrigger id="problem-other-user-submission-access" className="rounded-2xl">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                {otherUserSubmissionAccessOptions.map((option) => (
                  <SelectItem key={option.value} value={option.value}>
                    {option.label}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
            <p className="text-xs text-slate-500">
              {otherUserSubmissionAccessOptions.find((option) => option.value === model.otherUserSubmissionAccess)?.description}
            </p>
          </div>
          <Button
            type="button"
            className="rounded-2xl bg-slate-950 text-white hover:bg-slate-800"
            disabled={model.isSaving}
            onClick={() => {
              void model.saveAccess()
            }}
          >
            {model.isSaving ? t('problem.detail.savingAccess') : t('problem.detail.saveAccess')}
          </Button>
          {model.accessErrorMessage ? (
            <Alert variant="destructive" className="rounded-2xl border-rose-200 bg-rose-50/95">
              <AlertDescription className="text-rose-700">{model.accessErrorMessage}</AlertDescription>
            </Alert>
          ) : null}
          {model.accessSuccessMessage ? (
            <Alert className="rounded-2xl border-emerald-200 bg-emerald-50/95">
              <AlertDescription className="text-emerald-700">{model.accessSuccessMessage}</AlertDescription>
            </Alert>
          ) : null}
        </div>
      </DialogContent>
    </Dialog>
  )
}
