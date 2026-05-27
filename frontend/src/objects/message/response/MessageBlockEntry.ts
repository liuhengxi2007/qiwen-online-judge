import type { UserIdentity } from '@/objects/user/UserIdentity'

export type MessageBlockEntry = {
  user: UserIdentity
  createdAt: string
}
