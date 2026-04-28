import type { UserIdentity } from '@/features/auth/domain/auth'

export type MessageBlockEntry = {
  user: UserIdentity
  createdAt: string
}
