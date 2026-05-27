import type { UserIdentity } from '@/objects/user/UserIdentity'
import { UserProfileLink } from '@/pages/components/user/user-profile-link'
import { useI18n } from '@/system/i18n/use-i18n'

type SignedInUserProps = {
  user: UserIdentity
  className?: string
}

export function SignedInUser({ user, className }: SignedInUserProps) {
  const { t } = useI18n()

  return (
    <p className={className ?? 'text-sm text-slate-600'}>
      <span>{t('common.signedInAsLabel')} </span>
      <UserProfileLink showUsername user={user} />
      <span>.</span>
    </p>
  )
}
