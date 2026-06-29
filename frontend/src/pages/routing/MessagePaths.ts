import type { Username } from '@/objects/user/Username'
import { usernameValue } from '@/objects/user/Username'

/**
 * 生成与指定用户的私信会话路径；用户名会经过 URL 编码以保留特殊字符边界。
 */
export function messageConversationPath(username: Username): string {
  return `/messages/with/${encodeURIComponent(usernameValue(username))}`
}
