import { Link } from 'react-router-dom'

import { displayNameValue, usernameValue, type UserDisplayMode } from '@/features/auth/domain/auth'
import type { UserIdentity } from '@/features/auth/model/UserIdentity'
import { useAuthStore } from '@/features/auth/stores/use-auth-store'

type UserProfileLinkProps = {
  user: UserIdentity
  showUsername?: boolean
  stacked?: boolean
  className?: string
}

export function UserProfileLink({
  user,
  showUsername,
  stacked = false,
  className,
}: UserProfileLinkProps) {
  const viewerDisplayMode = useAuthStore((state) => state.session?.preferences.displayMode ?? 'display_name')
  const profilePath = `/user/${usernameValue(user.username)}`
  const wrapperClassName = stacked ? 'inline-flex flex-col gap-1' : 'inline-flex items-baseline gap-2'
  const displayName = displayNameValue(user.displayName)
  const username = usernameValue(user.username)
  const displayMode: UserDisplayMode = showUsername ? 'display_name_with_username' : viewerDisplayMode

  function renderLabel() {
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

  return (
    <span className={className ?? wrapperClassName}>
      <Link className="font-medium text-slate-900 hover:underline" to={profilePath}>
        {renderLabel()}
      </Link>
    </span>
  )
}
