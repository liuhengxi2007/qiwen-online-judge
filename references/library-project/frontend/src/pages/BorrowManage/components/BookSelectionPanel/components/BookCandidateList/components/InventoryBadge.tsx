import { Badge } from '@/components/ui/badge'
import { cn } from '@/components/ui/utils'

export function InventoryBadge({
  canBorrow,
  label,
}: {
  canBorrow: boolean
  label: string
}) {
  return (
    <Badge
      variant="outline"
      className={cn(
        'rounded-full px-2.5 py-1 text-xs font-medium',
        canBorrow && 'border-emerald-200 bg-emerald-50 text-emerald-700',
        !canBorrow && 'border-slate-200 bg-slate-50 text-slate-500',
      )}
    >
      {canBorrow ? label : '不可借'}
    </Badge>
  )
}
