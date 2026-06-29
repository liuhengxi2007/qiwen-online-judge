import type { DisplayName } from '@/objects/user/DisplayName'
import { displayNameValue } from '@/objects/user/DisplayName'
import type { UserAvatarUrl } from '@/objects/user/UserAvatarUrl'
import { userAvatarUrlValue } from '@/objects/user/UserAvatarUrl'
import { useI18n } from '@/system/i18n/use-i18n'

/**
 * 用户资料概览面板属性，包含头像、显示名和加载状态。
 */
type ProfileOverviewPanelProps = {
  avatarUrl: UserAvatarUrl | null
  isLoadingProfile: boolean
  profileName: string
  profileDisplayName: DisplayName | null
}

/**
 * 用户资料概览面板，展示头像或首字母占位，以及当前显示名。
 */
export function ProfileOverviewPanel({
  avatarUrl,
  isLoadingProfile,
  profileDisplayName,
  profileName,
}: ProfileOverviewPanelProps) {
  const { t } = useI18n()

  return (
    <div className="grid gap-5 rounded-3xl bg-slate-50 p-6 sm:grid-cols-[10rem_minmax(0,1fr)] sm:items-start">
      {profileDisplayName ? (
        <ProfilePageAvatar avatarUrl={avatarUrl} displayName={profileDisplayName} />
      ) : null}
      <div className="min-w-0 pt-1">
        <p className="text-sm text-slate-500">{t('common.displayName')}</p>
        <p className="mt-2 text-2xl font-semibold text-slate-900">{isLoadingProfile ? t('common.loading') : profileName}</p>
      </div>
    </div>
  )
}

/**
 * 用户资料头像展示组件，优先使用头像 URL，缺失时用显示名首字母生成占位。
 */
function ProfilePageAvatar({
  avatarUrl,
  displayName,
}: {
  avatarUrl: UserAvatarUrl | null
  displayName: DisplayName
}) {
  const fallback = displayNameValue(displayName).trim().slice(0, 1).toUpperCase() || '?'

  return (
    <div className="h-40 overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm">
      {avatarUrl ? (
        <img
          alt={displayNameValue(displayName)}
          className="size-full object-cover"
          src={userAvatarUrlValue(avatarUrl)}
        />
      ) : (
        <div className="flex size-full items-center justify-center bg-violet-100 text-5xl font-semibold text-violet-800">
          {fallback}
        </div>
      )}
    </div>
  )
}
