import { AlertTriangle, CheckCircle2, Info } from 'lucide-react'

import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { cn } from '@/components/ui/utils'

import type { BorrowRuleCheck } from '../objects/BorrowRuleCheck'

export function BorrowRulePanel({ checks }: { checks: BorrowRuleCheck[] }) {
  return (
    <Card className="gap-0 rounded-lg border-slate-200 py-0 shadow-sm">
      <CardHeader className="border-b border-slate-200 px-5 py-4">
        <CardTitle className="text-base text-slate-950">借阅规则检查</CardTitle>
        <CardDescription>规则结果由当前读者、借阅草稿和历史记录共同计算。</CardDescription>
      </CardHeader>
      <CardContent className="space-y-3 px-5 py-4">
        {checks.map((check) => (
          <div key={check.id} className="flex gap-3 rounded-lg border border-slate-200 bg-white p-3">
            <div
              className={cn(
                'mt-0.5 flex size-8 shrink-0 items-center justify-center rounded-full',
                check.severity === 'pass' && 'bg-emerald-50 text-emerald-700',
                check.severity === 'warning' && 'bg-amber-50 text-amber-700',
                check.severity === 'block' && 'bg-red-50 text-red-700',
              )}
            >
              {check.severity === 'pass' ? <CheckCircle2 className="size-4" /> : null}
              {check.severity === 'warning' ? <Info className="size-4" /> : null}
              {check.severity === 'block' ? <AlertTriangle className="size-4" /> : null}
            </div>
            <div className="min-w-0 space-y-1">
              <div className="text-sm font-medium text-slate-900">{check.title}</div>
              <div className="text-sm leading-6 text-slate-500">{check.description}</div>
            </div>
          </div>
        ))}
      </CardContent>
    </Card>
  )
}
