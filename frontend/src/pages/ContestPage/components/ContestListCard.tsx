import { Link } from 'react-router-dom'
import { CalendarDays, CalendarPlus } from 'lucide-react'

import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import type { ContestSummary } from '@/objects/contest/response/ContestSummary'
import { PaginationControls } from '@/pages/components/PaginationControls'
import { useI18n } from '@/system/i18n/use-i18n'

import { ContestListItem } from './ContestListItem'

type ContestListCardProps = {
  activeRegistrationSlug: string | null
  canCreate: boolean
  contests: ContestSummary[]
  currentPage: number
  isLoading: boolean
  now: number
  onPageChange: (page: number) => void
  onToggleRegistration: (contest: ContestSummary) => void
  totalPages: number
}

/**
 * 比赛列表卡片，展示创建入口、列表状态、比赛条目和分页。
 */
export function ContestListCard({
  activeRegistrationSlug,
  canCreate,
  contests,
  currentPage,
  isLoading,
  now,
  onPageChange,
  onToggleRegistration,
  totalPages,
}: ContestListCardProps) {
  const { t } = useI18n()

  return (
    <Card className="border-slate-200 bg-white shadow-[0_24px_60px_rgba(15,23,42,0.08)]">
      <CardHeader>
        <div className="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
          <div className="flex items-center gap-3">
            <div className="flex size-12 items-center justify-center rounded-2xl bg-cyan-100 text-cyan-700">
              <CalendarDays className="size-5" />
            </div>
            <div>
              <CardTitle className="text-xl text-slate-950">{t('contest.list.cardTitle')}</CardTitle>
              <CardDescription>{t('contest.list.cardDescription')}</CardDescription>
            </div>
          </div>
          {canCreate ? (
            <Button asChild variant="create">
              <Link to="/contests/new">
                <CalendarPlus className="size-4" />
                {t('contest.list.create')}
              </Link>
            </Button>
          ) : null}
        </div>
      </CardHeader>
      <CardContent className="space-y-4">
        {isLoading ? (
          <p className="text-sm text-slate-500">{t('contest.list.loading')}</p>
        ) : contests.length === 0 ? (
          <div className="rounded-3xl border border-dashed border-slate-300 bg-slate-50 px-6 py-10 text-center">
            <p className="text-base font-medium text-slate-900">{t('contest.list.emptyTitle')}</p>
            <p className="mt-2 text-sm leading-7 text-slate-600">{t('contest.list.emptyDescription')}</p>
          </div>
        ) : (
          contests.map((contest) => (
            <ContestListItem
              key={contest.id}
              activeRegistrationSlug={activeRegistrationSlug}
              contest={contest}
              now={now}
              onToggleRegistration={onToggleRegistration}
            />
          ))
        )}
        {!isLoading && contests.length > 0 && totalPages > 1 ? (
          <PaginationControls
            currentPage={currentPage}
            totalPages={totalPages}
            previousLabel={t('common.pagination.previous')}
            nextLabel={t('common.pagination.next')}
            onPageChange={onPageChange}
          />
        ) : null}
      </CardContent>
    </Card>
  )
}
