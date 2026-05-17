import type { ReactNode } from 'react'

import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
  AlertDialogTrigger,
} from '@/components/ui/alert-dialog'
import { useI18n } from '@/shared/i18n/use-i18n'

type ConfirmActionDialogProps = {
  open?: boolean
  onOpenChange?: (open: boolean) => void
  trigger?: ReactNode
  title: string
  description: string
  confirmLabel: string
  cancelLabel?: string
  onConfirm: () => void
  destructive?: boolean
}

export function ConfirmActionDialog({
  open,
  onOpenChange,
  trigger,
  title,
  description,
  confirmLabel,
  cancelLabel,
  onConfirm,
  destructive = false,
}: ConfirmActionDialogProps) {
  const { t } = useI18n()

  return (
    <AlertDialog open={open} onOpenChange={onOpenChange}>
      {trigger ? <AlertDialogTrigger asChild>{trigger}</AlertDialogTrigger> : null}
      <AlertDialogContent className="rounded-[2rem] border-slate-200 bg-white p-7 shadow-[0_28px_80px_rgba(15,23,42,0.18)]">
        <AlertDialogHeader className="gap-3">
          <AlertDialogTitle className="text-xl text-slate-950">{title}</AlertDialogTitle>
          <AlertDialogDescription className="text-sm leading-7 text-slate-600">
            {description}
          </AlertDialogDescription>
        </AlertDialogHeader>
        <AlertDialogFooter className="gap-3">
          <AlertDialogCancel className="rounded-full border-slate-300 bg-white px-5">{
            cancelLabel ?? t('common.cancel')
          }</AlertDialogCancel>
          <AlertDialogAction
            className={
              destructive
                ? 'rounded-full bg-rose-600 px-5 text-white hover:bg-rose-700'
                : 'rounded-full bg-slate-950 px-5 text-white hover:bg-slate-800'
            }
            onClick={onConfirm}
          >
            {confirmLabel}
          </AlertDialogAction>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  )
}
