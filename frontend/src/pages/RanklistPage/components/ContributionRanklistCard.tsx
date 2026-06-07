import { Medal, Trophy } from 'lucide-react'

import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { UserProfileLink } from '@/pages/components/UserProfileLink'
import { contributionTextClassName } from '@/pages/objects/UserDisplayLabel'
import { userContributionValue } from '@/objects/user/UserContribution'
import type { UserContributionRanklistItem } from '@/objects/user/response/UserContributionRanklistItem'
import { useI18n } from '@/system/i18n/use-i18n'

import { RanklistPagination } from './RanklistPagination'

type ContributionRanklistCardProps = {
  acceptedPage: number
  contributionPage: number
  errorMessage: string
  items: UserContributionRanklistItem[]
  isLoading: boolean
  pageSize: number
  ratingPage: number
  totalPages: number
}

export function ContributionRanklistCard({
  acceptedPage,
  contributionPage,
  errorMessage,
  items,
  isLoading,
  pageSize,
  ratingPage,
  totalPages,
}: ContributionRanklistCardProps) {
  const { t } = useI18n()

  return (
    <Card className="border-slate-200 bg-white/95 shadow-[0_24px_60px_rgba(15,23,42,0.08)]">
      <CardHeader>
        <CardTitle className="text-xl text-slate-950">{t('ranklist.contributionTitle')}</CardTitle>
      </CardHeader>
      <CardContent className="space-y-4 p-5 pt-0 sm:p-6 sm:pt-0">
        {isLoading ? (
          <p className="text-sm text-slate-500">{t('ranklist.loading')}</p>
        ) : errorMessage ? (
          <div className="rounded-3xl border border-dashed border-slate-300 bg-slate-50 px-6 py-10 text-center">
            <p className="text-base font-medium text-slate-900">{t('ranklist.unavailable')}</p>
          </div>
        ) : items.length > 0 ? (
          items.map((item, index) => {
            const rank = (contributionPage - 1) * pageSize + index + 1
            const contribution = Math.round(userContributionValue(item.contribution))
            return (
              <div
                key={item.user.username}
                className="grid gap-4 rounded-3xl border border-slate-200 bg-slate-50 p-5 sm:grid-cols-[5rem_1fr_9rem] sm:items-center"
              >
                <div className="flex items-center gap-2 text-slate-700">
                  {rank <= 3 ? <Trophy className="size-4 text-amber-600" /> : <Medal className="size-4 text-slate-400" />}
                  <span className="text-lg font-semibold">#{rank}</span>
                </div>
                <UserProfileLink user={item.user} />
                <div className="text-left sm:text-right">
                  <p className="text-xs uppercase tracking-[0.18em] text-slate-400">{t('ranklist.contribution')}</p>
                  <p className={`mt-1 text-2xl font-semibold ${contributionTextClassName(contribution)}`}>{contribution}</p>
                </div>
              </div>
            )
          })
        ) : (
          <div className="rounded-3xl border border-dashed border-slate-300 bg-slate-50 px-6 py-10 text-center">
            <p className="text-base font-medium text-slate-900">{t('ranklist.empty')}</p>
          </div>
        )}

        {!isLoading && !errorMessage && items.length > 0 ? (
          <RanklistPagination
            acceptedPage={acceptedPage}
            contributionPage={contributionPage}
            currentPage={contributionPage}
            ratingPage={ratingPage}
            target="contribution"
            totalPages={totalPages}
          />
        ) : null}
      </CardContent>
    </Card>
  )
}
