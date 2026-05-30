import { Link } from 'react-router-dom'

import { Button } from '@/components/ui/button'
import { useI18n } from '@/system/i18n/use-i18n'

import { pagePath } from '../functions/PageQuery'

type RanklistPaginationProps = {
  acceptedPage: number
  contributionPage: number
  currentPage: number
  totalPages: number
  target: 'accepted' | 'contribution'
}

export function RanklistPagination({
  acceptedPage,
  contributionPage,
  currentPage,
  totalPages,
  target,
}: RanklistPaginationProps) {
  const { t } = useI18n()
  const canGoPrevious = currentPage > 1
  const canGoNext = currentPage < totalPages
  const previousContributionPage = target === 'contribution' ? currentPage - 1 : contributionPage
  const previousAcceptedPage = target === 'accepted' ? currentPage - 1 : acceptedPage
  const nextContributionPage = target === 'contribution' ? currentPage + 1 : contributionPage
  const nextAcceptedPage = target === 'accepted' ? currentPage + 1 : acceptedPage

  return (
    <div className="flex flex-wrap items-center justify-between gap-3 pt-2">
      <p className="text-sm text-slate-500">
        {t('ranklist.pageStatus', { page: String(currentPage), totalPages: String(totalPages) })}
      </p>
      <div className="flex gap-2">
        <Button asChild={canGoPrevious} disabled={!canGoPrevious} variant="outline" className="rounded-2xl">
          {canGoPrevious ? (
            <Link to={pagePath(previousContributionPage, previousAcceptedPage)}>{t('submission.pagination.previous')}</Link>
          ) : (
            <span>{t('submission.pagination.previous')}</span>
          )}
        </Button>
        <Button asChild={canGoNext} disabled={!canGoNext} variant="outline" className="rounded-2xl">
          {canGoNext ? (
            <Link to={pagePath(nextContributionPage, nextAcceptedPage)}>{t('submission.pagination.next')}</Link>
          ) : (
            <span>{t('submission.pagination.next')}</span>
          )}
        </Button>
      </div>
    </div>
  )
}
