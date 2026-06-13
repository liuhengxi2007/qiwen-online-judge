import { Link } from 'react-router-dom'

import { usernameValue } from '@/objects/user/Username'
import type { UserIdentity } from '@/objects/user/UserIdentity'
import { useAuthStore } from '@/pages/stores/auth/UseAuthStore'
import { formatUserDisplayLabel } from '@/pages/objects/UserDisplayLabel'

/**
 * 用户资料链接属性，输入用户身份和可选布局、样式控制。
 */
type UserProfileLinkProps = {
  user: UserIdentity
  showUsername?: boolean
  stacked?: boolean
  className?: string
  linkClassName?: string
}

/**
 * 按当前查看者偏好渲染用户资料链接，目标路径使用用户名构造。
 */
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
