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
              Signed in as {displayNameValue(signedInDisplayName)} ({usernameValue(signedInUsername)}).
            </p>
          </div>

          {canManageProblems ? (
            <div className="flex flex-wrap gap-3">
              <Button
                type="button"
                variant={managementPanel === 'edit' ? 'default' : 'outline'}
                className={
                  managementPanel === 'edit'
                    ? 'rounded-2xl bg-amber-600 text-white hover:bg-amber-700'
                    : 'rounded-2xl border-amber-300 bg-white text-amber-800 hover:bg-amber-50'
                }
                onClick={() => {
                  onTogglePanel('edit')
                }}
              >
                Edit problem set
              </Button>
              <Button
                type="button"
                variant={managementPanel === 'access' ? 'default' : 'outline'}
                className={
                  managementPanel === 'access'
                    ? 'rounded-2xl bg-teal-700 text-white hover:bg-teal-800'
                    : 'rounded-2xl border-teal-300 bg-white text-teal-800 hover:bg-teal-50'
                }
                onClick={() => {
                  onTogglePanel('access')
                }}
              >
                Access management
              </Button>
            </div>
          ) : null}
        </div>
      </CardHeader>
      <CardContent className="space-y-4">
        <div className="flex flex-wrap items-center gap-3">
          <Badge variant="secondary">{resourceAccessBadgeLabel(problemSet.accessPolicy)}</Badge>
        </div>
        <div className="rounded-3xl border border-slate-200 bg-slate-50 px-6 py-6">
          {problemSetDescriptionValue(problemSet.description) ? (
            <MarkdownDocument content={problemSetDescriptionValue(problemSet.description)} />
          ) : (
            <p className="text-sm text-slate-500">No description provided.</p>
          )}
        </div>
        <p className="text-xs uppercase tracking-[0.18em] text-slate-400">
          Owner {usernameValue(problemSet.ownerUsername)}
        </p>
      </CardContent>
    </Card>
  )
}
