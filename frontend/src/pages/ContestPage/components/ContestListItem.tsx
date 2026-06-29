import { Link } from 'react-router-dom'

import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { contestDescriptionValue } from '@/objects/contest/ContestDescription'
import { contestSlugValue } from '@/objects/contest/ContestSlug'
import { contestTitleValue } from '@/objects/contest/ContestTitle'
import type { ContestSummary } from '@/objects/contest/response/ContestSummary'
import { DateTimeText } from '@/pages/components/DateTimeText'
import { UserProfileLink } from '@/pages/components/UserProfileLink'
import { resourceAccessBadgeLabel } from '@/pages/objects/ResourceAccessDisplay'
import { useI18n } from '@/system/i18n/use-i18n'

type ContestListItemProps = {
  activeRegistrationSlug: string | null
  contest: ContestSummary
  now: number
  onToggleRegistration: (contest: ContestSummary) => void
}

/**
 * 单个比赛列表项，集中展示基础信息和报名状态按钮。
 */
export function ContestListItem({ activeRegistrationSlug, contest, now, onToggleRegistration }: ContestListItemProps) {
  const { t } = useI18n()
  const hasStarted = now >= new Date(contest.startAt).getTime()
  const isRegistered = contest.registrationStatus.isRegistered
  const isUpdating = activeRegistrationSlug === contest.slug
  const isLocked = hasStarted
  const contestPath = `/contests/${contestSlugValue(contest.slug)}`

  return (
    <div className="rounded-3xl border border-slate-200 bg-slate-50 p-5">
      <div className="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
        <div className="flex flex-wrap items-center gap-3">
          {contest.canViewDetail ? (
            <Link className="text-lg font-semibold text-slate-950 hover:underline" to={contestPath}>
              {contestTitleValue(contest.title)}
            </Link>
          ) : (
            <span className="text-lg font-semibold text-slate-500">{contestTitleValue(contest.title)}</span>
          )}
          <Badge variant="secondary">{resourceAccessBadgeLabel(contest.accessPolicy, t)}</Badge>
          {!contest.canViewDetail ? <Badge variant="outline">{t('contest.list.detailUnavailable')}</Badge> : null}
        </div>
        <Button
          type="button"
          disabled={isUpdating || isLocked}
          variant={isRegistered && !hasStarted ? 'destructiveOutline' : isRegistered || hasStarted ? 'outline' : 'default'}
          className={
            isRegistered && hasStarted
              ? 'rounded-2xl border-emerald-200 bg-white text-emerald-700 hover:bg-white hover:text-emerald-700'
              : hasStarted
                ? 'rounded-2xl border-slate-200 bg-white text-slate-500 hover:bg-white hover:text-slate-500'
                : undefined
          }
          onClick={() => {
            if (!isLocked) {
              onToggleRegistration(contest)
            }
          }}
        >
          {isUpdating
            ? t('contest.list.registrationUpdating')
            : isRegistered && hasStarted
              ? t('contest.list.registered')
              : isRegistered
                ? t('contest.list.unregister')
                : hasStarted
                  ? t('contest.list.registrationClosed')
                  : t('contest.list.register')}
        </Button>
      </div>
      <p className="mt-2 font-mono text-sm text-slate-500">{contestSlugValue(contest.slug)}</p>
      <p className="mt-3 text-sm leading-7 text-slate-600">
        {contestDescriptionValue(contest.description) || t('common.noDescription')}
      </p>
      <div className="mt-4 grid gap-3 text-sm text-slate-600 sm:grid-cols-2">
        <p>
          <span className="font-medium text-slate-900">{t('contest.list.startAt')} </span>
          <DateTimeText value={contest.startAt} />
        </p>
        <p>
          <span className="font-medium text-slate-900">{t('contest.list.endAt')} </span>
          <DateTimeText value={contest.endAt} />
        </p>
      </div>
      <p className="mt-4 text-xs uppercase tracking-[0.18em] text-slate-400">
        <span>{t('common.authorLabel')} </span>
        {contest.author ? (
          <UserProfileLink
            className="inline-flex items-baseline gap-2 normal-case tracking-normal"
            showUsername
            user={contest.author}
          />
        ) : (
          <span className="normal-case tracking-normal">{t('common.noAuthor')}</span>
        )}
      </p>
    </div>
  )
}
