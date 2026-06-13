import { Gauge } from 'lucide-react'

import type { RatingValue } from '@/objects/rating/RatingValue'
import { formatRatingValue } from '@/objects/rating/RatingValue'
import { useI18n } from '@/system/i18n/use-i18n'

/**
 * Rating 面板属性，displayedRating 为 null 时根据加载状态展示 loading 或占位。
 */
type RatingPanelProps = {
  displayedRating: RatingValue | null
  isLoadingProfile: boolean
}

/**
 * 用户 rating 统计面板，展示格式化后的 rating 或资料不可用占位。
 */
export function RatingPanel({ displayedRating, isLoadingProfile }: RatingPanelProps) {
  const { t } = useI18n()

  return (
    <div className="rounded-3xl bg-amber-50 p-6">
      <div className="flex items-center gap-2 text-amber-800">
        <Gauge className="size-4" />
        <p className="text-sm font-medium">{t('userProfile.rating')}</p>
      </div>
      <p className="mt-2 text-3xl font-semibold text-amber-800">
        {displayedRating === null ? (isLoadingProfile ? t('common.loading') : '--') : formatRatingValue(displayedRating)}
      </p>
      <p className="mt-1 text-sm text-amber-700">{t('userProfile.ratingDescription')}</p>
    </div>
  )
}
