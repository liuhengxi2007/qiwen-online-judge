import type { BorrowDraft } from '../../../objects/BorrowDraft'
import { BorrowDraftItemRow } from './BorrowDraftItemRow'

export function BorrowDraftList({ draft }: { draft: BorrowDraft }) {
  if (draft.items.length === 0) {
    return (
      <div className="rounded-lg border border-slate-200 bg-slate-50 p-5 text-sm text-slate-500">
        还没有加入待借图书，请先从图书选择面板加入草稿。
      </div>
    )
  }

  return (
    <div className="overflow-hidden rounded-lg border border-slate-200 bg-white">
      {draft.items.map((item) => (
        <BorrowDraftItemRow key={item.book.id} item={item} draft={draft} />
      ))}
    </div>
  )
}
