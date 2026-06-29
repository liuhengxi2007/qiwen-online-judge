import type { Username } from '@/objects/user/Username'

/** 创建私信会话请求体；targetUsername 指定对话另一方。 */
export type CreateConversationRequest = {
  targetUsername: Username
}
