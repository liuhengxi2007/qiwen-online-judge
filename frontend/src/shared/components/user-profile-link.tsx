import { Link } from 'react-router-dom'

import { usernameValue } from '@/features/auth/domain/auth'
import type { UserIdentity } from '@/features/auth/model/UserIdentity'
import { useAuthStore } from '@/features/auth/stores/use-auth-store'
import { formatUserDisplayLabel } from '@/shared/components/user-display-label'

type UserProfileLinkProps = {
  user: UserIdentity
  showUsername?: boolean
  stacked?: boolean
  className?: string
}

export function UserProfileLink({
  user,
  stacked = false,
  className,
}: UserProfileLinkProps) {
  const viewerDisplayMode = useAuthStore((state) => state.session?.preferences.displayMode ?? 'display_name')
  const profilePath = `/user/${usernameValue(user.username)}`
  const wrapperClassName = stacked ? 'inline-flex flex-col gap-1' : 'inline-flex items-baseline gap-2'

  return (
    <span className={className ?? wrapperClassName}>
      <Link className="font-medium text-slate-900 hover:underline" to={profilePath}>
        {formatUserDisplayLabel(user, viewerDisplayMode)}
      </Link>
    </span>
  )
}
