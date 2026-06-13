import type { UserIdentity } from '@/objects/user/UserIdentity'

/** 消息屏蔽列表条目；记录被屏蔽用户和创建时间。 */
export type MessageBlockEntry = {
  user: UserIdentity
  createdAt: string
}
