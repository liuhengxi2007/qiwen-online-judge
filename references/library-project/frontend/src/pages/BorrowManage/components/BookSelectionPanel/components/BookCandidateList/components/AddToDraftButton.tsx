import { Plus, Check } from 'lucide-react'

import { Button } from '@/components/ui/button'

export function AddToDraftButton({
  canBorrow,
  isInDraft,
  onAdd,
}: {
  canBorrow: boolean
  isInDraft: boolean
  onAdd: () => void
}) {
  return (
    <Button
      type="button"
      size="sm"
      variant={isInDraft ? 'secondary' : 'outline'}
      className="h-8 rounded-lg"
      disabled={!canBorrow || isInDraft}
      onClick={onAdd}
    >
      {isInDraft ? <Check className="size-4" /> : <Plus className="size-4" />}
      {isInDraft ? '已加入' : '加入草稿'}
    </Button>
  )
}
