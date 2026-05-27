import { displayNameValue, usernameValue } from '@/objects/user/user-parsers'
import type { UserDisplayMode } from '@/objects/user/UserDisplayMode'
import type { UserIdentity } from '@/objects/user/UserIdentity'

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
