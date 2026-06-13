import type { UserIdentity } from '@/objects/user/UserIdentity'

/** 按通过题数排行的条目；只暴露用户公开身份和通过数量。 */
export type UserAcceptedRanklistItem = {
  user: UserIdentity
  acceptedCount: number
}
