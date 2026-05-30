import { AlertTriangle, CheckCircle2, UserRound } from 'lucide-react'

import { cn } from '@/components/ui/utils'

import type { ReaderPanelState } from '../../../objects/BorrowManagePageState'
import type { ReaderEligibility } from '../objects/ReaderEligibility'

export function ReaderProfileCard({
  reader,
  eligibility,
}: {
  reader: ReaderPanelState
  eligibility: ReaderEligibility
}) {
  return (
    <div className="rounded-lg border border-slate-200 bg-slate-50 p-4">
      <div className="flex items-start gap-3">
        <div className="flex size-10 shrink-0 items-center justify-center rounded-full bg-white text-slate-700 shadow-sm">
          <UserRound className="size-5" />
        </div>
        <div className="min-w-0 flex-1 space-y-2">
          <div>
            <div className="truncate text-sm font-semibold text-slate-950">
              {reader.readerName.trim() || '未选择读者'}
            </div>
            <div className="mt-1 text-xs text-slate-500">读者状态根据本页借阅记录实时计算</div>
          </div>

          <div
            className={cn(
              'inline-flex items-center gap-2 rounded-full px-3 py-1 text-xs font-medium',
              eligibility.tone === 'ready' && 'bg-emerald-50 text-emerald-700',
              eligibility.tone === 'blocked' && 'bg-red-50 text-red-700',
            )}
          >
            {eligibility.tone === 'ready' ? <CheckCircle2 className="size-3.5" /> : <AlertTriangle className="size-3.5" />}
            {eligibility.title}
          </div>
        </div>
      </div>

      <p className="mt-3 text-sm leading-6 text-slate-500">{eligibility.description}</p>
    </div>
  )
}
