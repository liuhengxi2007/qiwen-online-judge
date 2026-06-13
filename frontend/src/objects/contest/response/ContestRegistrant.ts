import type { UserIdentity } from '@/objects/user/UserIdentity'

/** 比赛报名用户条目；只暴露公开身份。 */
export type ContestRegistrant = {
  user: UserIdentity
}
