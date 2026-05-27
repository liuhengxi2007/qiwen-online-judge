import { ShieldCheck } from 'lucide-react'

import { Alert, AlertDescription } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { ResourceAccessEditor } from '@/pages/components/resource-access-editor'
import type { ResourceAccessPolicy } from '@/objects/shared/access/ResourceAccessPolicy'
import { resourceAccessSummary } from '@/pages/objects/resource-access-display'
import { useI18n } from '@/system/i18n/use-i18n'

type ProblemSetAccessDialogProps = {
  open: boolean
  accessPolicy: ResourceAccessPolicy
  summaryPolicy: ResourceAccessPolicy
  grantedUsersInput: string
  grantedGroupsInput: string
  isSaving: boolean
  errorMessage: string
  successMessage: string
  onOpenChange: (open: boolean) => void
  onBaseAccessChange: (value: ResourceAccessPolicy['baseAccess']) => void
  onGrantedUsersInputChange: (value: string) => void
  onGrantedGroupsInputChange: (value: string) => void
  onSave: () => void
}

export function ProblemSetAccessDialog({
  open,
  accessPolicy,
  summaryPolicy,
  grantedUsersInput,
  grantedGroupsInput,
  isSaving,
  errorMessage,
  successMessage,
  onOpenChange,
  onBaseAccessChange,
  onGrantedUsersInputChange,
  onGrantedGroupsInputChange,
  onSave,
}: ProblemSetAccessDialogProps) {
  const { t } = useI18n()
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
            onBaseAccessChange={onBaseAccessChange}
            onGrantedUsersInputChange={onGrantedUsersInputChange}
            onGrantedGroupsInputChange={onGrantedGroupsInputChange}
          />
          <Button type="button" className="rounded-2xl bg-slate-950 text-white hover:bg-slate-800" disabled={isSaving} onClick={onSave}>
            {isSaving ? t('problemSet.detail.savingAccess') : t('problemSet.detail.saveAccess')}
          </Button>
          {errorMessage ? (
            <Alert variant="destructive" className="rounded-2xl border-rose-200 bg-rose-50/95">
              <AlertDescription className="text-rose-700">{errorMessage}</AlertDescription>
            </Alert>
          ) : null}
          {successMessage ? (
            <Alert className="rounded-2xl border-emerald-200 bg-emerald-50/95">
              <AlertDescription className="text-emerald-700">{successMessage}</AlertDescription>
            </Alert>
          ) : null}
        </div>
      </DialogContent>
    </Dialog>
  )
}
