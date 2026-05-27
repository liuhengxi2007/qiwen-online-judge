import { Sparkles } from 'lucide-react'

import { contributionTextClassName } from '@/objects/user/user-display-label'
import { useI18n } from '@/system/i18n/use-i18n'

type ContributionPanelProps = {
  displayedContribution: number | null
  isLoadingProfile: boolean
}

export function ContributionPanel({ displayedContribution, isLoadingProfile }: ContributionPanelProps) {
  const { t } = useI18n()

  return (
    <div className="rounded-3xl bg-violet-50 p-6">
      <div className="flex items-center gap-2 text-violet-800">
        <Sparkles className="size-4" />
        <p className="text-sm font-medium">{t('userProfile.contribution')}</p>
      </div>
      <p className={`mt-2 text-3xl font-semibold ${displayedContribution === null ? 'text-violet-950' : contributionTextClassName(displayedContribution)}`}>
        {displayedContribution === null ? (isLoadingProfile ? t('common.loading') : '--') : String(displayedContribution)}
      </p>
      <p className="mt-1 text-sm text-violet-700">{t('userProfile.contributionDescription')}</p>
    </div>
  )
}
