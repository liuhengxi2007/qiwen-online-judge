import type { ContestSlug } from '@/objects/contest/ContestSlug'

/** 追加 rating 比赛请求体；m 为 rating 公式参数，由后端解释。 */
export type AppendRatingContestRequest = {
  contestSlug: ContestSlug
  m: number
}
