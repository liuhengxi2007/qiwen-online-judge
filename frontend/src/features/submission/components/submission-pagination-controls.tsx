import { Button } from '@/components/ui/button'
import { useI18n } from '@/shared/i18n/i18n'

type SubmissionPaginationControlsProps = {
  currentPage: number
  totalPages: number
  pageNumbers: number[]
  onPageChange: (page: number) => void
}

export function SubmissionPaginationControls({
  currentPage,
  totalPages,
  pageNumbers,
  onPageChange,
}: SubmissionPaginationControlsProps) {
  const { t } = useI18n()

  if (totalPages <= 1) {
    return null
  }

  return (
    <div className="flex flex-wrap items-center justify-center gap-2 pt-4">
      <Button
        type="button"
        variant="outline"
        className="rounded-2xl border-slate-300 bg-white"
        disabled={currentPage === 1}
        onClick={() => onPageChange(Math.max(1, currentPage - 1))}
      >
        {t('submission.pagination.previous')}
      </Button>
      {pageNumbers.map((page) => (
        <Button
          key={page}
          type="button"
          variant={page === currentPage ? 'default' : 'outline'}
          className={
            page === currentPage
              ? 'rounded-2xl bg-slate-950 text-white'
              : 'rounded-2xl border-slate-300 bg-white'
          }
          onClick={() => onPageChange(page)}
        >
          {page}
        </Button>
      ))}
      <Button
        type="button"
        variant="outline"
        className="rounded-2xl border-slate-300 bg-white"
        disabled={currentPage === totalPages}
        onClick={() => onPageChange(Math.min(totalPages, currentPage + 1))}
      >
        {t('submission.pagination.next')}
      </Button>
    </div>
  )
}
