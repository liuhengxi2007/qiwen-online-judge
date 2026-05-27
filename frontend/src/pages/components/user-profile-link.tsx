import { Link } from 'react-router-dom'

import { usernameValue } from '@/objects/user/Username'
import type { UserIdentity } from '@/objects/user/UserIdentity'
import { useAuthStore } from '@/pages/stores/auth/use-auth-store'
import { formatUserDisplayLabel } from '@/pages/objects/user-display-label'

type UserProfileLinkProps = {
  user: UserIdentity
  showUsername?: boolean
  stacked?: boolean
  className?: string
  linkClassName?: string
}

export function UserProfileLink({
  user,
  stacked = false,
  className,
  linkClassName,
}: UserProfileLinkProps) {
  const viewerDisplayMode = useAuthStore((state) => state.session?.preferences.displayMode ?? 'display_name')
  const profilePath = `/user/${usernameValue(user.username)}`
  const wrapperClassName = stacked ? 'inline-flex flex-col gap-1' : 'inline-flex items-baseline gap-2'

  return (
    <span className={className ?? wrapperClassName}>
      <Link className={linkClassName ?? 'font-medium text-slate-900 hover:underline'} to={profilePath}>
        {formatUserDisplayLabel(user, viewerDisplayMode)}
      </Link>
    </span>
  )
}
