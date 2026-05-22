import type { UserIdentity } from '@/features/user/domain/user'

export type MessageBlockEntry = {
  user: UserIdentity
  createdAt: string
}
