import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'

import type { ReaderPanelState } from '../../objects/BorrowManagePageState'
import { ReaderBorrowStats } from './components/ReaderBorrowStats'
import { ReaderProfileCard } from './components/ReaderProfileCard'
import { ReaderSearchBox } from './components/ReaderSearchBox'
import { useReaderPanel } from './hooks/useReaderPanel'

export function ReaderPanel({ reader }: { reader: ReaderPanelState }) {
  const eligibility = useReaderPanel(reader)

  return (
    <Card className="gap-0 rounded-lg border-slate-200 py-0 shadow-sm">
      <CardHeader className="border-b border-slate-200 px-5 py-4">
        <CardTitle className="text-base text-slate-950">读者面板</CardTitle>
        <CardDescription>复杂组件内部可以继续放自己的 hooks 和 objects。</CardDescription>
      </CardHeader>
      <CardContent className="space-y-4 px-5 py-4">
        <ReaderSearchBox readerName={reader.readerName} onReaderNameChange={reader.setReaderName} />
        <ReaderProfileCard reader={reader} eligibility={eligibility} />
        <ReaderBorrowStats reader={reader} />
      </CardContent>
    </Card>
  )
}
