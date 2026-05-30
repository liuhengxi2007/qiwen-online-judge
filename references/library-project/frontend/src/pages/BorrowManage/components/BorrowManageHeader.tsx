import { AlertTriangle, BookOpenCheck, ClipboardList, Users } from 'lucide-react'

import type { BorrowManageSummary } from '../objects/BorrowManageSummary'

export function BorrowManageHeader({ summary }: { summary: BorrowManageSummary }) {
  return (
    <header className="border-b border-slate-200 bg-white">
      <div className="mx-auto flex w-full max-w-7xl flex-col gap-5 px-6 py-6 lg:flex-row lg:items-center lg:justify-between">
        <div className="space-y-2">
          <div className="inline-flex items-center gap-2 text-sm font-medium text-slate-500">
            <BookOpenCheck className="size-4" />
            借阅流转工作台
          </div>
          <div className="space-y-1">
            <h1 className="text-2xl font-semibold text-slate-950">借书管理</h1>
            <p className="text-sm text-slate-500">
              从读者核验、图书选择、规则检查到借书确认，按业务区域组织页面。
            </p>
          </div>
        </div>

        <div className="grid gap-3 sm:grid-cols-4 lg:min-w-[560px]">
          <SummaryItem icon={<Users className="size-4" />} label="当前读者" value={summary.readerName} />
          <SummaryItem icon={<ClipboardList className="size-4" />} label="草稿图书" value={`${summary.draftCount} 本`} />
          <SummaryItem icon={<BookOpenCheck className="size-4" />} label="活跃借阅" value={`${summary.activeBorrowCount} 条`} />
          <SummaryItem
            icon={<AlertTriangle className="size-4" />}
            label="规则提醒"
            value={`${summary.blockedRuleCount} 阻断 / ${summary.warningRuleCount} 提醒`}
          />
        </div>
      </div>
    </header>
  )
}

function SummaryItem({
  icon,
  label,
  value,
}: {
  icon: React.ReactNode
  label: string
  value: string
}) {
  return (
    <div className="rounded-lg border border-slate-200 bg-slate-50 px-3 py-3">
      <div className="flex items-center gap-2 text-xs text-slate-500">
        {icon}
        {label}
      </div>
      <div className="mt-2 truncate text-sm font-semibold text-slate-900">{value}</div>
    </div>
  )
}
