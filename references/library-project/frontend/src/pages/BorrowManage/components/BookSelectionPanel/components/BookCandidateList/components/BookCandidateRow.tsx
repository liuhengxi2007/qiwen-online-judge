import type { BookCandidateView } from '../../../objects/BookCandidateView'
import { AddToDraftButton } from './AddToDraftButton'
import { InventoryBadge } from './InventoryBadge'

export function BookCandidateRow({
  candidate,
  onAdd,
}: {
  candidate: BookCandidateView
  onAdd: () => void
}) {
  return (
    <div className="grid gap-3 border-b border-slate-100 px-4 py-4 last:border-b-0 lg:grid-cols-[1fr_120px_auto] lg:items-center">
      <div className="min-w-0 space-y-1">
        <div className="truncate text-sm font-semibold text-slate-950">《{candidate.book.title}》</div>
        <div className="text-xs leading-5 text-slate-500">
          {candidate.book.author} / {candidate.book.categoryLabel} / ISBN {candidate.book.isbn}
        </div>
      </div>

      <InventoryBadge canBorrow={candidate.canBorrow} label={candidate.inventoryLabel} />

      <div className="flex justify-start lg:justify-end">
        <AddToDraftButton
          canBorrow={candidate.canBorrow}
          isInDraft={candidate.isInDraft}
          onAdd={onAdd}
        />
      </div>
    </div>
  )
}
