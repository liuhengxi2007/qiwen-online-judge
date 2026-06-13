import { cn } from '@/components/ui/class-names'
import { Button } from '@/components/ui/button'

/**
 * 通用分页控件属性，调用方负责提供页码窗口和切页回调。
 */
type PaginationControlsProps = {
  currentPage: number
  pageNumbers: number[]
  totalPages: number
  onPageChange: (page: number) => void
  previousLabel: string
  nextLabel: string
  tone?: 'slate' | 'stone'
  className?: string
}

/**
 * 渲染上一页、页码窗口和下一页按钮；点击时通过回调请求目标页。
 */
export function PaginationControls({
  currentPage,
  pageNumbers,
  totalPages,
  onPageChange,
  previousLabel,
  nextLabel,
  tone = 'slate',
  className,
}: PaginationControlsProps) {
  const inactiveClassName =
    tone === 'stone' ? 'rounded-2xl border-stone-300 bg-white' : 'rounded-2xl border-slate-300 bg-white'
  const activeClassName = tone === 'stone' ? 'rounded-2xl bg-stone-950 text-white' : 'rounded-2xl bg-slate-950 text-white'

  return (
    <div className={cn('flex flex-wrap items-center justify-center gap-2 pt-4', className)}>
      <Button
        type="button"
        variant="outline"
        className={inactiveClassName}
        disabled={currentPage === 1}
        onClick={() => onPageChange(Math.max(1, currentPage - 1))}
      >
        {previousLabel}
      </Button>
      {pageNumbers.map((page) => (
        <Button
          key={page}
          type="button"
          variant={page === currentPage ? 'default' : 'outline'}
          className={page === currentPage ? activeClassName : inactiveClassName}
          onClick={() => onPageChange(page)}
        >
          {page}
        </Button>
      ))}
      <Button
        type="button"
        variant="outline"
        className={inactiveClassName}
        disabled={currentPage === totalPages}
        onClick={() => onPageChange(Math.min(totalPages, currentPage + 1))}
      >
        {nextLabel}
      </Button>
    </div>
  )
}
