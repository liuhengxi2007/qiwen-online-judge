import { Avatar, AvatarFallback, AvatarImage } from '@/components/ui/avatar'
import { cn } from '@/components/ui/class-names'
import { displayNameValue } from '@/objects/user/DisplayName'
import type { DisplayName } from '@/objects/user/DisplayName'
import { userAvatarUrlValue } from '@/objects/user/UserAvatarUrl'
import type { UserAvatarUrl } from '@/objects/user/UserAvatarUrl'

/**
 * 用户头像组件属性，包含头像 URL、显示名和 fallback 样式扩展。
 */
type UserAvatarProps = {
  avatarUrl: UserAvatarUrl | null
  className?: string
  displayName: DisplayName
  fallbackClassName?: string
}

/**
 * 渲染用户头像；有头像 URL 时展示图片，否则使用显示名首字母作为占位。
 */
export function UserAvatar({ avatarUrl, className, displayName, fallbackClassName }: UserAvatarProps) {
  const label = displayNameValue(displayName).trim()
  const fallback = label ? label.slice(0, 1).toUpperCase() : '?'

  return (
    <Avatar className={cn('border border-slate-200 bg-slate-100 shadow-sm', className)}>
      {avatarUrl ? <AvatarImage alt={label} src={userAvatarUrlValue(avatarUrl)} /> : null}
      <AvatarFallback className={cn('bg-violet-100 text-sm font-semibold text-violet-800', fallbackClassName)}>
        {fallback}
      </AvatarFallback>
    </Avatar>
  )
}
