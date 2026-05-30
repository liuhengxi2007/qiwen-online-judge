import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'

import type { BorrowDraft } from '../../objects/BorrowDraft'
import { BorrowDraftList } from './components/BorrowDraftList'

export function BorrowDraftPanel({ draft }: { draft: BorrowDraft }) {
  return (
    <Card className="gap-0 rounded-lg border-slate-200 py-0 shadow-sm">
      <CardHeader className="border-b border-slate-200 px-5 py-4">
        <div className="flex items-start justify-between gap-4">
          <div>
            <CardTitle className="text-base text-slate-950">借阅草稿</CardTitle>
            <CardDescription>草稿区的子组件统一放入当前区域自己的 components 目录。</CardDescription>
          </div>
          <div className="rounded-full bg-slate-100 px-3 py-1 text-xs font-medium text-slate-600">
            {draft.itemCount} 本
          </div>
        </div>
      </CardHeader>
      <CardContent className="px-5 py-4">
        <BorrowDraftList draft={draft} />
      </CardContent>
    </Card>
  )
}
