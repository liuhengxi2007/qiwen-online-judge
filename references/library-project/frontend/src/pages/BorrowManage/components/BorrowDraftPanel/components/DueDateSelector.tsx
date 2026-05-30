import { Input } from '@/components/ui/input'

export function DueDateSelector({
  dueDate,
  onDueDateChange,
}: {
  dueDate: string
  onDueDateChange: (value: string) => void
}) {
  return (
    <Input
      type="date"
      value={dueDate}
      className="h-9 rounded-lg border-slate-200"
      onChange={(event) => onDueDateChange(event.target.value)}
    />
  )
}
