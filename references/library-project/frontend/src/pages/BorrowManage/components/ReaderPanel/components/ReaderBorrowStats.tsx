import type { ReaderPanelState } from '../../../objects/BorrowManagePageState'

export function ReaderBorrowStats({ reader }: { reader: ReaderPanelState }) {
  return (
    <div className="grid grid-cols-3 gap-3">
      <ReaderStat label="借阅中" value={reader.borrowedCount} />
      <ReaderStat label="已逾期" value={reader.overdueCount} />
      <ReaderStat label="已归还" value={reader.returnedCount} />
    </div>
  )
}

function ReaderStat({ label, value }: { label: string; value: number }) {
  return (
    <div className="rounded-lg border border-slate-200 bg-white px-3 py-3">
      <div className="text-xs text-slate-500">{label}</div>
      <div className="mt-1 text-lg font-semibold text-slate-950">{value}</div>
    </div>
  )
}
