import type { UserIdentity } from '@/features/user/model/UserIdentity'

export type MessageBlockEntry = {
  user: UserIdentity
  createdAt: string
}
