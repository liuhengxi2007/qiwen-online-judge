import { X } from 'lucide-react'

import { Button } from '@/components/ui/button'

export function RemoveDraftItemButton({ onRemove }: { onRemove: () => void }) {
  return (
    <Button
      type="button"
      variant="ghost"
      size="icon"
      className="size-8 rounded-lg text-slate-500 hover:text-red-700"
      onClick={onRemove}
      aria-label="移除草稿图书"
    >
      <X className="size-4" />
    </Button>
  )
}
