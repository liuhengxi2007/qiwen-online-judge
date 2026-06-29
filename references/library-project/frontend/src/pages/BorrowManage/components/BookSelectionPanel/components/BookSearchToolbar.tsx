import { RefreshCw, Search } from 'lucide-react'

import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'
import { Switch } from '@/components/ui/switch'

import type { BookSelectionState } from '../../../objects/BorrowManagePageState'

export function BookSearchToolbar({ books }: { books: BookSelectionState }) {
  return (
    <div className="grid gap-3 lg:grid-cols-[1fr_180px_auto_auto]">
      <div className="relative">
        <Search className="pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2 text-slate-400" />
        <Input
          value={books.keyword}
          className="h-10 rounded-lg border-slate-200 pl-9"
          placeholder="搜索书名、作者或 ISBN"
          onChange={(event) => books.setKeyword(event.target.value)}
        />
      </div>

      <Select value={books.category} onValueChange={books.setCategory}>
        <SelectTrigger className="h-10 rounded-lg border-slate-200">
          <SelectValue placeholder="全部分类" />
        </SelectTrigger>
        <SelectContent>
          <SelectItem value="all">全部分类</SelectItem>
          {books.categories.map((category) => (
            <SelectItem key={category} value={category}>
              {category}
            </SelectItem>
          ))}
        </SelectContent>
      </Select>

      <Label className="h-10 justify-between rounded-lg border border-slate-200 bg-white px-3 text-slate-600">
        仅可借
        <Switch checked={books.availableOnly} onCheckedChange={books.setAvailableOnly} />
      </Label>

      <Button
        type="button"
        variant="outline"
        className="h-10 rounded-lg border-slate-200"
        disabled={books.loading}
        onClick={() => void books.refresh()}
      >
        <RefreshCw className="size-4" />
        刷新
      </Button>
    </div>
  )
}
