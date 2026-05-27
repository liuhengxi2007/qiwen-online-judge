import { Link } from 'react-router-dom'
import { FilePlus2, LibraryBig } from 'lucide-react'

import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import type { ProblemSummary } from '@/objects/problem/response/ProblemSummary'
import { useI18n } from '@/system/i18n/use-i18n'

import { ProblemListItem } from './ProblemListItem'

type ProblemListCardProps = {
  canCreate: boolean
  currentPage: number
  isLoading: boolean
  onApplyQuery: () => void
  onClearQuery: () => void
  onPageChange: (page: number) => void
  pageNumbers: number[]
  problems: ProblemSummary[]
  queryInput: string
  setQueryInput: (value: string) => void
  showSlugSupplement: boolean
  totalPages: number
}

export function ProblemListCard({
  canCreate,
  currentPage,
  isLoading,
  onApplyQuery,
  onClearQuery,
  onPageChange,
  pageNumbers,
  problems,
  queryInput,
  setQueryInput,
  showSlugSupplement,
  totalPages,
}: ProblemListCardProps) {
  const { t } = useI18n()

  return (
    <Card className="border-slate-200 bg-white shadow-[0_24px_60px_rgba(15,23,42,0.08)]">
      <CardHeader>
        <div className="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
          <div className="flex items-center gap-3">
            <div className="flex size-12 items-center justify-center rounded-2xl bg-emerald-100 text-emerald-700">
              <LibraryBig className="size-5" />
            </div>
            <div>
              <CardTitle className="text-xl text-slate-950">{t('problem.list.cardTitle')}</CardTitle>
              <CardDescription>
                {t('problem.list.cardDescription')}
              </CardDescription>
            </div>
          </div>
          {canCreate ? (
            <Button asChild className="rounded-2xl bg-emerald-300 text-emerald-950 hover:bg-emerald-400">
              <Link to="/problems/new">
                <FilePlus2 className="size-4" />
                {t('problem.list.create')}
              </Link>
            </Button>
          ) : null}
        </div>
      </CardHeader>
      <CardContent className="space-y-4">
        <div className="flex flex-col gap-3 sm:flex-row sm:items-end">
          <div className="flex-1 space-y-2">
            <Label htmlFor="problem-search">{t('problem.list.searchLabel')}</Label>
            <Input
              id="problem-search"
              value={queryInput}
              placeholder={t('problem.list.searchPlaceholder')}
              onChange={(event) => {
                setQueryInput(event.target.value)
              }}
              onKeyDown={(event) => {
                if (event.key === 'Enter') {
                  event.preventDefault()
                  onApplyQuery()
                }
              }}
            />
          </div>
          <div className="flex gap-3">
            <Button type="button" className="rounded-2xl bg-slate-950 text-white hover:bg-slate-800" onClick={onApplyQuery}>
              {t('problem.list.searchApply')}
            </Button>
            <Button
              type="button"
              variant="outline"
              className="rounded-2xl border-slate-300 bg-white"
              onClick={onClearQuery}
            >
              {t('problem.list.searchClear')}
            </Button>
          </div>
        </div>

        {isLoading ? (
          <p className="text-sm text-slate-500">{t('problem.list.loading')}</p>
        ) : problems.length === 0 ? (
          <div className="rounded-3xl border border-dashed border-slate-300 bg-slate-50 px-6 py-10 text-center">
            <p className="text-base font-medium text-slate-900">{t('problem.list.emptyTitle')}</p>
            <p className="mt-2 text-sm leading-7 text-slate-600">
              {t('problem.list.emptyDescription')}
            </p>
          </div>
        ) : (
          problems.map((problem) => (
            <ProblemListItem key={problem.id} problem={problem} showSlugSupplement={showSlugSupplement} />
          ))
        )}

        {!isLoading && problems.length > 0 && totalPages > 1 ? (
          <div className="flex flex-wrap items-center justify-center gap-2 pt-4">
            <Button
              type="button"
              variant="outline"
              className="rounded-2xl border-slate-300 bg-white"
              disabled={currentPage === 1}
              onClick={() => onPageChange(Math.max(1, currentPage - 1))}
            >
              {t('common.pagination.previous')}
            </Button>
            {pageNumbers.map((page) => (
              <Button
                key={page}
                type="button"
                variant={page === currentPage ? 'default' : 'outline'}
                className={page === currentPage ? 'rounded-2xl bg-slate-950 text-white' : 'rounded-2xl border-slate-300 bg-white'}
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
              {t('common.pagination.next')}
            </Button>
          </div>
        ) : null}
      </CardContent>
    </Card>
  )
}
