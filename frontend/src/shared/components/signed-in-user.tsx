import type { UserIdentity } from '@/features/user/model/UserIdentity'
import { UserProfileLink } from '@/shared/components/user-profile-link'
import { useI18n } from '@/shared/i18n/i18n'

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
