import type { Username } from '@/features/user/domain/user'

export type CreateConversationRequest = {
  targetUsername: Username
}
