import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'

import type { BorrowDraft } from '../../objects/BorrowDraft'
import type { BookSelectionState } from '../../objects/BorrowManagePageState'
import { BookCandidateList } from './components/BookCandidateList'
import { BookSearchToolbar } from './components/BookSearchToolbar'
import { useBookSelectionPanel } from './hooks/useBookSelectionPanel'

export function BookSelectionPanel({
  books,
  draft,
}: {
  books: BookSelectionState
  draft: BorrowDraft
}) {
  const selection = useBookSelectionPanel(books, draft)

  return (
    <Card className="gap-0 rounded-lg border-slate-200 py-0 shadow-sm">
      <CardHeader className="border-b border-slate-200 px-5 py-4">
        <div className="flex flex-col gap-2 sm:flex-row sm:items-start sm:justify-between">
          <div>
            <CardTitle className="text-base text-slate-950">图书选择面板</CardTitle>
            <CardDescription>这个区域继续拆出搜索工具栏、候选列表和列表行。</CardDescription>
          </div>
          <div className="text-sm text-slate-500">
            可借候选 {selection.availableCandidateCount} 本 / 草稿 {selection.selectedCount} 本
          </div>
        </div>
      </CardHeader>
      <CardContent className="space-y-4 px-5 py-4">
        <BookSearchToolbar books={books} />
        <BookCandidateList
          candidates={selection.candidates}
          draft={draft}
          loading={books.loading}
          errorMessage={books.errorMessage}
        />
      </CardContent>
    </Card>
  )
}
