import type { RatingContestListItem } from '@/objects/rating/response/RatingContestListItem'

/** Rating 管理状态；列出已纳入 rating 计算的比赛。 */
export type RatingManageState = {
  contests: RatingContestListItem[]
}
