import type { Username } from '@/features/auth/domain/auth'

export type CreateConversationRequest = {
  targetUsername: Username
}
