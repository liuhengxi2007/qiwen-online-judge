import { AlertTriangle, CalendarClock } from 'lucide-react'
import { Link } from 'react-router-dom'

import { Badge } from '@/components/ui/badge'
import type { RatingContestListItem } from '@/objects/rating/response/RatingContestListItem'
import { contestSlugValue } from '@/objects/contest/ContestSlug'
import { contestTitleValue } from '@/objects/contest/ContestTitle'
import { UserProfileLink } from '@/pages/components/UserProfileLink'
import { formatDateTime, formatUtcOffsetTitle } from '@/system/format/date-time'
import { useI18n } from '@/system/i18n/use-i18n'

type RatingContestSequenceProps = {
  contests: RatingContestListItem[]
  isLoading: boolean
}

export function RatingContestSequence({ contests, isLoading }: RatingContestSequenceProps) {
  const { t } = useI18n()

  if (isLoading) {
    return <p className="text-sm text-slate-500">{t('ratingManage.loading')}</p>
  }

  if (contests.length === 0) {
    return (
      <div className="rounded-3xl border border-dashed border-slate-300 bg-slate-50 px-6 py-10 text-center">
        <p className="text-base font-medium text-slate-900">{t('ratingManage.emptyTitle')}</p>
        <p className="mt-1 text-sm text-slate-500">{t('ratingManage.emptyDescription')}</p>
      </div>
    )
  }

  return (
    <div className="space-y-3">
      {contests.map((contest) => (
        <div
          key={`${contest.position}-${contestSlugValue(contest.contestSlug)}`}
          className="grid gap-4 rounded-3xl border border-slate-200 bg-slate-50 p-5 lg:grid-cols-[5rem_minmax(0,1fr)_13rem]"
        >
          <div>
            <p className="text-xs uppercase tracking-[0.18em] text-slate-400">{t('ratingManage.position')}</p>
            <p className="mt-1 text-2xl font-semibold text-slate-900">#{contest.position}</p>
          </div>

          <div className="min-w-0">
            <div className="flex flex-wrap items-center gap-2">
              <Link
                className="min-w-0 truncate text-lg font-semibold text-slate-950 hover:underline"
                to={`/contests/${contestSlugValue(contest.contestSlug)}`}
              >
                {contestTitleValue(contest.contestTitle)}
              </Link>
              {contest.overlapWarning ? (
                <Badge variant="outline" className="border-amber-300 bg-amber-50 text-amber-800">
                  <AlertTriangle className="size-3" />
                  {t('ratingManage.overlapWarning')}
                </Badge>
              ) : null}
            </div>
            <p className="mt-1 text-sm text-slate-500">{contestSlugValue(contest.contestSlug)}</p>
            <div className="mt-3 flex flex-wrap gap-2">
              <Badge variant="secondary">{t('ratingManage.mValue', { m: String(contest.m) })}</Badge>
              <Badge variant="secondary">
                {t('ratingManage.participantCount', { count: String(contest.participantCount) })}
              </Badge>
            </div>
          </div>

          <div className="space-y-2 text-sm text-slate-600">
            <div className="flex items-start gap-2">
              <CalendarClock className="mt-0.5 size-4 text-slate-400" />
              <div>
                <p title={formatUtcOffsetTitle(contest.contestStartAt)}>
                  {t('ratingManage.startsAt', { value: formatDateTime(contest.contestStartAt) })}
                </p>
                <p title={formatUtcOffsetTitle(contest.contestEndAt)}>
                  {t('ratingManage.endsAt', { value: formatDateTime(contest.contestEndAt) })}
                </p>
              </div>
            </div>
            <p>
              {t('ratingManage.appendedBy')}{' '}
              {contest.appendedBy ? <UserProfileLink user={contest.appendedBy} /> : t('ratingManage.deletedUser')}
            </p>
            <p title={formatUtcOffsetTitle(contest.appendedAt)}>
              {t('ratingManage.appendedAt', { value: formatDateTime(contest.appendedAt) })}
            </p>
          </div>
        </div>
      ))}
    </div>
  )
}
