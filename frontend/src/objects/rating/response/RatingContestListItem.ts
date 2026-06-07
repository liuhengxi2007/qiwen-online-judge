import type { ContestSlug } from '@/objects/contest/ContestSlug'
import type { ContestTitle } from '@/objects/contest/ContestTitle'
import type { UserIdentity } from '@/objects/user/UserIdentity'

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
