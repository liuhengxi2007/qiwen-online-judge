import type { ContestSlug } from '@/objects/contest/ContestSlug'

export type AppendRatingContestRequest = {
  contestSlug: ContestSlug
  m: number
}
