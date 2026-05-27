import { Link2 } from 'lucide-react'

import { Alert, AlertDescription } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { useI18n } from '@/system/i18n/use-i18n'

type ProblemSetLinkProblemCardProps = {
  linkProblemSlug: string
  activeLink: boolean
  linkErrorMessage: string
  linkSuccessMessage: string
  onLinkProblemSlugChange: (value: string) => void
  onAttachProblem: () => void
}

export function ProblemSetLinkProblemCard({
  linkProblemSlug,
  activeLink,
  linkErrorMessage,
  linkSuccessMessage,
  onLinkProblemSlugChange,
  onAttachProblem,
}: ProblemSetLinkProblemCardProps) {
  const { t } = useI18n()

  return (
    <div className="rounded-[1.75rem] border border-slate-200 bg-slate-50 p-6">
      <div className="flex items-center gap-3">
        <div className="flex size-12 items-center justify-center rounded-2xl bg-sky-100 text-sky-700">
          <Link2 className="size-5" />
        </div>
        <div>
          <h2 className="text-xl font-semibold text-slate-950">{t('problemSet.detail.linkProblemTitle')}</h2>
          <p className="text-sm text-slate-600">{t('problemSet.detail.linkProblemDescription')}</p>
        </div>
      </div>
      <div className="mt-5 space-y-3">
        {linkErrorMessage ? (
          <Alert variant="destructive" className="rounded-2xl border-rose-200 bg-rose-50/95">
            <AlertDescription className="text-rose-700">{linkErrorMessage}</AlertDescription>
          </Alert>
        ) : null}
        {linkSuccessMessage ? (
          <Alert className="rounded-2xl border-emerald-200 bg-emerald-50/95">
            <AlertDescription className="text-emerald-700">{linkSuccessMessage}</AlertDescription>
          </Alert>
        ) : null}
        <div className="space-y-2">
          <Label htmlFor="link-problem-slug">{t('problemSet.detail.linkProblemSlug')}</Label>
          <Input
            id="link-problem-slug"
            value={linkProblemSlug}
            onChange={(event) => {
              onLinkProblemSlugChange(event.target.value)
            }}
          />
        </div>
        <Button
          type="button"
          variant="outline"
          className="rounded-2xl border-slate-300 bg-white"
          disabled={activeLink}
          onClick={onAttachProblem}
        >
          {activeLink ? t('problemSet.detail.linkingProblem') : t('problemSet.detail.linkProblem')}
        </Button>
      </div>
    </div>
  )
}
