import { Link } from 'react-router-dom'

import { displayNameValue, usernameValue } from '@/features/auth/domain/auth'
import type { UserIdentity } from '@/features/auth/model/UserIdentity'

type UserProfileLinkProps = {
  user: UserIdentity
  showUsername?: boolean
  stacked?: boolean
  className?: string
}

export function UserProfileLink({
  user,
  showUsername = false,
  stacked = false,
  className,
}: UserProfileLinkProps) {
  const profilePath = `/user/${usernameValue(user.username)}`
  const wrapperClassName = stacked ? 'inline-flex flex-col gap-1' : 'inline-flex items-baseline gap-2'

  return (
    <span className={className ?? wrapperClassName}>
      <Link className="font-medium text-slate-900 hover:underline" to={profilePath}>
        {displayNameValue(user.displayName)}
      </Link>
      {showUsername ? <span className="font-mono text-xs text-slate-500">{usernameValue(user.username)}</span> : null}
    </span>
  )
}
