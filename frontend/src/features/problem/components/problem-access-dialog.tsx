import { ShieldCheck } from 'lucide-react'

import { Alert, AlertDescription } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { Label } from '@/components/ui/label'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import type { useProblemDetailPageModel } from '@/features/problem/hooks/use-problem-detail-page-model'
import { ResourceAccessEditor } from '@/shared/components/resource-access-editor'
import { resourceAccessSummary } from '@/shared/domain/resource-lifecycle'
import { useI18n } from '@/shared/i18n/use-i18n'

type ProblemDetailPageModel = ReturnType<typeof useProblemDetailPageModel>

type ProblemAccessDialogProps = {
  model: ProblemDetailPageModel
  open: boolean
  othersSubmissionAccessOptions: ReadonlyArray<{ value: 'none' | 'summary' | 'detail'; label: string; description: string }>
  setOpen: (open: boolean) => void
}

export function ProblemAccessDialog({
  model,
  open,
  othersSubmissionAccessOptions,
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
            <Label htmlFor="problem-others-submission-access">{t('problem.create.othersSubmissionAccess')}</Label>
            <Select value={model.othersSubmissionAccess} onValueChange={model.setOthersSubmissionAccess}>
              <SelectTrigger id="problem-others-submission-access" className="rounded-2xl">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                {othersSubmissionAccessOptions.map((option) => (
                  <SelectItem key={option.value} value={option.value}>
                    {option.label}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
            <p className="text-xs text-slate-500">
              {othersSubmissionAccessOptions.find((option) => option.value === model.othersSubmissionAccess)?.description}
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
