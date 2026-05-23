import { displayNameValue, usernameValue } from '@/features/user/lib/user-parsers'
import type { UserDisplayMode } from '@/features/user/model/UserDisplayMode'
import type { UserIdentity } from '@/features/user/model/UserIdentity'

type UserDisplayIdentity = Pick<UserIdentity, 'displayName' | 'username'>

export function formatUserDisplayLabel(user: UserDisplayIdentity, displayMode: UserDisplayMode): string {
  const displayName = displayNameValue(user.displayName)
  const username = usernameValue(user.username)

  switch (displayMode) {
    case 'username':
      return username
    case 'display_name_with_username':
      return `${displayName} (${username})`
    case 'display_name':
    default:
      return displayName
  }
}
