import { Search } from 'lucide-react'

import { Input } from '@/components/ui/input'

export function ReaderSearchBox({
  readerName,
  onReaderNameChange,
}: {
  readerName: string
  onReaderNameChange: (value: string) => void
}) {
  return (
    <div className="space-y-2">
      <label className="text-sm font-medium text-slate-700" htmlFor="borrow-reader-name">
        借阅人
      </label>
      <div className="relative">
        <Search className="pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2 text-slate-400" />
        <Input
          id="borrow-reader-name"
          value={readerName}
          className="h-11 rounded-lg border-slate-200 pl-9"
          placeholder="输入读者姓名或编号"
          onChange={(event) => onReaderNameChange(event.target.value)}
        />
      </div>
    </div>
  )
}
