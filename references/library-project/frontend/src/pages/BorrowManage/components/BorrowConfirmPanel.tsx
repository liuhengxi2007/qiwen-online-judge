import { Loader2, Send } from 'lucide-react'

import { Alert, AlertDescription } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'

import type { BorrowConfirmationState } from '../objects/BorrowManagePageState'
import type { BorrowManageSummary } from '../objects/BorrowManageSummary'

export function BorrowConfirmPanel({
  confirmation,
  summary,
}: {
  confirmation: BorrowConfirmationState
  summary: BorrowManageSummary
}) {
  return (
    <Card className="gap-0 rounded-lg border-slate-200 py-0 shadow-sm">
      <CardHeader className="border-b border-slate-200 px-5 py-4">
        <CardTitle className="text-base text-slate-950">借书确认</CardTitle>
        <CardDescription>提交前汇总读者、草稿和规则状态。</CardDescription>
      </CardHeader>
      <CardContent className="space-y-4 px-5 py-4">
        <div className="grid gap-3 sm:grid-cols-3">
          <ConfirmItem label="读者" value={summary.readerName} />
          <ConfirmItem label="待借图书" value={`${summary.draftCount} 本`} />
          <ConfirmItem label="阻断规则" value={`${summary.blockedRuleCount} 项`} />
        </div>

        {confirmation.message ? (
          <Alert className="rounded-lg border-slate-200 bg-slate-50">
            <AlertDescription className="text-sm text-slate-700">{confirmation.message}</AlertDescription>
          </Alert>
        ) : null}

        <Button
          type="button"
          className="h-11 w-full rounded-lg bg-slate-900 text-white hover:bg-slate-800"
          disabled={!confirmation.canSubmit}
          onClick={() => void confirmation.submit()}
        >
          {confirmation.submitting ? <Loader2 className="size-4 animate-spin" /> : <Send className="size-4" />}
          提交借书登记
        </Button>
      </CardContent>
    </Card>
  )
}

function ConfirmItem({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-lg border border-slate-200 bg-slate-50 px-3 py-3">
      <div className="text-xs text-slate-500">{label}</div>
      <div className="mt-1 truncate text-sm font-semibold text-slate-900">{value}</div>
    </div>
  )
}
