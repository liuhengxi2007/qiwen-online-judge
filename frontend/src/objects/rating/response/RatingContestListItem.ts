import type { ContestSlug } from '@/objects/contest/ContestSlug'
import type { ContestTitle } from '@/objects/contest/ContestTitle'
import type { UserIdentity } from '@/objects/user/UserIdentity'

/** Rating 管理中的比赛条目；包含比赛时间、参数、参与人数和追加人。 */
export type RatingContestListItem = {
  position: number
  contestSlug: ContestSlug
  contestTitle: ContestTitle
  contestStartAt: string
  contestEndAt: string
  m: number
  participantCount: number
  overlapWarning: boolean
  appendedBy: UserIdentity | null
  appendedAt: string
}
