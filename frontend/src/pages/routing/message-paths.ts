import type { Username } from '@/objects/user/Username'
import { usernameValue } from '@/objects/user/Username'

export function messageConversationPath(username: Username): string {
  return `/messages/with/${encodeURIComponent(usernameValue(username))}`
}
