import { BookSelectionPanel } from './components/BookSelectionPanel'
import { BorrowConfirmPanel } from './components/BorrowConfirmPanel'
import { BorrowDraftPanel } from './components/BorrowDraftPanel'
import { BorrowManageHeader } from './components/BorrowManageHeader'
import { BorrowRecordPanel } from './components/BorrowRecordPanel'
import { BorrowRulePanel } from './components/BorrowRulePanel'
import { ReaderPanel } from './components/ReaderPanel'
import { useBorrowManagePage } from './hooks/useBorrowManagePage'

export default function BorrowManage() {
  const page = useBorrowManagePage()

  return (
    <main className="min-h-screen bg-slate-50">
      <BorrowManageHeader summary={page.summary} />

      <section className="mx-auto grid w-full max-w-7xl gap-6 px-6 py-6 lg:grid-cols-[360px_1fr]">
        <div className="space-y-6">
          <ReaderPanel reader={page.reader} />
          <BorrowRulePanel checks={page.ruleChecks} />
        </div>

        <div className="space-y-6">
          <BookSelectionPanel books={page.books} draft={page.draft} />
          <BorrowDraftPanel draft={page.draft} />
          <BorrowConfirmPanel confirmation={page.confirmation} summary={page.summary} />
          <BorrowRecordPanel records={page.records} />
        </div>
      </section>
    </main>
  )
}
