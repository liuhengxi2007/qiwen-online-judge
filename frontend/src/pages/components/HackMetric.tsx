import type { ReactNode } from 'react'

/**
 * Hack 指标展示组件，输入标签和值，输出紧凑的边框指标块。
 */
export function HackMetric({ label, value }: { label: string; value: ReactNode }) {
  return (
    <div className="rounded-md border border-slate-200 px-3 py-2">
      <p className="text-xs uppercase tracking-normal text-slate-500">{label}</p>
      <div className="mt-1 font-medium text-slate-950">{value}</div>
    </div>
  )
}
