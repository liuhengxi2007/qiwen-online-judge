import { Gauge, Medal, Trophy } from 'lucide-react'

import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import type { RatingRanklistItem } from '@/objects/rating/response/RatingRanklistItem'
import { formatRatingValue } from '@/objects/rating/RatingValue'
import { UserProfileLink } from '@/pages/components/UserProfileLink'
import { useI18n } from '@/system/i18n/use-i18n'

import { RanklistPagination } from './RanklistPagination'

/**
 * Rating 榜卡片属性，包含当前页数据、加载/错误状态和三榜单页码上下文。
 */
type RatingRanklistCardProps = {
  acceptedPage: number
  contributionPage: number
  errorMessage: string
  items: RatingRanklistItem[]
  isLoading: boolean
  pageSize: number
  ratingPage: number
  totalPages: number
}

/**
 * Rating 榜卡片，按用户 rating 展示排名并在数据可用时渲染独立分页。
 */
export function RatingRanklistCard({
  acceptedPage,
  contributionPage,
  errorMessage,
  items,
  isLoading,
  pageSize,
  ratingPage,
  totalPages,
}: RatingRanklistCardProps) {
  // 保留扁平 props：榜单数据、加载状态和跨榜单分页页码是同一张卡片的公开边界。
  const { t } = useI18n()

  return (
    <Card className="border-slate-200 bg-white/95 shadow-[0_24px_60px_rgba(15,23,42,0.08)]">
      <CardHeader>
        <CardTitle className="flex items-center gap-2 text-xl text-slate-950">
          <Gauge className="size-5 text-amber-700" />
          {t('ranklist.ratingTitle')}
        </CardTitle>
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
            const rank = (ratingPage - 1) * pageSize + index + 1
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
                  <p className="text-xs uppercase tracking-[0.18em] text-slate-400">{t('ranklist.rating')}</p>
                  <p className="mt-1 text-2xl font-semibold text-amber-700">{formatRatingValue(item.rating)}</p>
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
            currentPage={ratingPage}
            ratingPage={ratingPage}
            target="rating"
            totalPages={totalPages}
          />
        ) : null}
      </CardContent>
    </Card>
  )
}
