import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { displayNameValue, usernameValue } from '@/features/auth/domain/auth'
import {
  problemSetDescriptionValue,
  problemSetSlugValue,
  problemSetTitleValue,
  type ProblemSetDetail,
} from '@/features/problemset/domain/problemset'
import { MarkdownDocument } from '@/shared/components/markdown-document'
import { resourceAccessBadgeLabel } from '@/shared/domain/resource-lifecycle'
import { useI18n } from '@/shared/i18n/i18n'

type ProblemSetDetailHeaderCardProps = {
  problemSet: ProblemSetDetail
  signedInDisplayName: Parameters<typeof displayNameValue>[0]
  signedInUsername: Parameters<typeof usernameValue>[0]
  canManageProblems: boolean
  managementPanel: 'edit' | 'access' | null
  onTogglePanel: (panel: 'edit' | 'access') => void
}

export function ProblemSetDetailHeaderCard({
  problemSet,
  signedInDisplayName,
  signedInUsername,
  canManageProblems,
  managementPanel,
  onTogglePanel,
}: ProblemSetDetailHeaderCardProps) {
  const { t } = useI18n()
  return (
    <Card className="border-slate-200 bg-white shadow-[0_24px_60px_rgba(15,23,42,0.08)]">
      <CardHeader>
        <div className="flex flex-col gap-4 lg:flex-row lg:items-start lg:justify-between">
          <div>
            <CardTitle className="text-2xl text-slate-950">{problemSetTitleValue(problemSet.title)}</CardTitle>
            <CardDescription className="mt-2 font-mono text-sm text-slate-500">
              {problemSetSlugValue(problemSet.slug)}
            </CardDescription>
            <p className="mt-3 text-sm text-slate-600">
              {t('common.signedInAs', {
                displayName: displayNameValue(signedInDisplayName),
                username: usernameValue(signedInUsername),
              })}
            </p>
          </div>

          {canManageProblems ? (
            <div className="flex flex-wrap gap-3">
              <Button
                type="button"
                variant={managementPanel === 'edit' ? 'default' : 'outline'}
                className={
                  managementPanel === 'edit'
                    ? 'rounded-2xl bg-rose-300 text-rose-950 hover:bg-rose-400'
                    : 'rounded-2xl border-rose-300 bg-white text-rose-900 hover:bg-rose-50'
                }
                onClick={() => {
                  onTogglePanel('edit')
                }}
              >
                {t('problemSet.detail.edit')}
              </Button>
              <Button
                type="button"
                variant={managementPanel === 'access' ? 'default' : 'outline'}
                className={
                  managementPanel === 'access'
                    ? 'rounded-2xl bg-rose-300 text-rose-950 hover:bg-rose-400'
                    : 'rounded-2xl border-rose-300 bg-white text-rose-900 hover:bg-rose-50'
                }
                onClick={() => {
                  onTogglePanel('access')
                }}
              >
                {t('problemSet.detail.access')}
              </Button>
            </div>
          ) : null}
        </div>
      </CardHeader>
      <CardContent className="space-y-4">
        <div className="flex flex-wrap items-center gap-3">
          <Badge variant="secondary">{resourceAccessBadgeLabel(problemSet.accessPolicy, t)}</Badge>
        </div>
        <div className="rounded-3xl border border-slate-200 bg-slate-50 px-6 py-6">
          {problemSetDescriptionValue(problemSet.description) ? (
            <MarkdownDocument content={problemSetDescriptionValue(problemSet.description)} />
          ) : (
            <p className="text-sm text-slate-500">{t('common.noDescription')}</p>
          )}
        </div>
        <p className="text-xs uppercase tracking-[0.18em] text-slate-400">
          {t('common.createdBy', { username: usernameValue(problemSet.creatorUsername) })}
        </p>
      </CardContent>
    </Card>
  )
}
