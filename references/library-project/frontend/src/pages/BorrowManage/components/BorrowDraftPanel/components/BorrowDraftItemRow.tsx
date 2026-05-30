import type { BorrowDraft } from '../../../objects/BorrowDraft'
import type { BorrowDraftItem } from '../../../objects/BorrowDraftItem'
import { DueDateSelector } from './DueDateSelector'
import { RemoveDraftItemButton } from './RemoveDraftItemButton'

export function BorrowDraftItemRow({
  item,
  draft,
}: {
  item: BorrowDraftItem
  draft: BorrowDraft
}) {
  return (
    <div className="grid gap-3 border-b border-slate-100 px-4 py-4 last:border-b-0 lg:grid-cols-[1fr_180px_auto] lg:items-center">
      <div className="min-w-0 space-y-1">
        <div className="truncate text-sm font-semibold text-slate-950">《{item.book.title}》</div>
        <div className="text-xs leading-5 text-slate-500">
          {item.book.author} / {item.book.categoryLabel} / {item.book.stockAvailable} 本可借
        </div>
      </div>

      <DueDateSelector
        dueDate={item.dueDate}
        onDueDateChange={(dueDate) => draft.updateDueDate(item.book.id, dueDate)}
      />

      <div className="flex justify-start lg:justify-end">
        <RemoveDraftItemButton onRemove={() => draft.removeBook(item.book.id)} />
      </div>
    </div>
  )
}
