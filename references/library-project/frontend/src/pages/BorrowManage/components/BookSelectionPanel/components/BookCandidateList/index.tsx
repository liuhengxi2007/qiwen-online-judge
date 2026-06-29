import { Alert, AlertDescription } from '@/components/ui/alert'

import type { BorrowDraft } from '../../../../objects/BorrowDraft'
import type { BookCandidateView } from '../../objects/BookCandidateView'
import { BookCandidateRow } from './components/BookCandidateRow'

export function BookCandidateList({
  candidates,
  draft,
  loading,
  errorMessage,
}: {
  candidates: BookCandidateView[]
  draft: BorrowDraft
  loading: boolean
  errorMessage: string
}) {
  if (loading) {
    return <div className="rounded-lg border border-slate-200 bg-slate-50 p-5 text-sm text-slate-500">正在加载图书候选...</div>
  }

  if (errorMessage) {
    return (
      <Alert className="rounded-lg border-red-200 bg-red-50">
        <AlertDescription className="text-sm text-red-700">{errorMessage}</AlertDescription>
      </Alert>
    )
  }

  if (candidates.length === 0) {
    return <div className="rounded-lg border border-slate-200 bg-slate-50 p-5 text-sm text-slate-500">没有符合条件的图书。</div>
  }

  return (
    <div className="overflow-hidden rounded-lg border border-slate-200 bg-white">
      {candidates.map((candidate) => (
        <BookCandidateRow
          key={candidate.book.id}
          candidate={candidate}
          onAdd={() => draft.addBook(candidate.book)}
        />
      ))}
    </div>
  )
}
