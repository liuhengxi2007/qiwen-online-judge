import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { submissionSourceCodeValue } from '@/features/submission/lib/submission-parsers'
import type { SubmissionSourceCode } from '@/features/submission/model/SubmissionSourceCode'
import { useI18n } from '@/shared/i18n/use-i18n'

type SubmissionSourceCodeCardProps = {
  sourceCode: SubmissionSourceCode
}

export function SubmissionSourceCodeCard({ sourceCode }: SubmissionSourceCodeCardProps) {
  const { t } = useI18n()

  return (
    <Card className="border-slate-200 bg-white shadow-[0_24px_60px_rgba(15,23,42,0.08)]">
      <CardHeader>
        <CardTitle className="text-xl text-slate-950">{t('submission.detail.sourceCode')}</CardTitle>
        <CardDescription>{t('submission.detail.sourceDescription')}</CardDescription>
      </CardHeader>
      <CardContent>
        <pre className="overflow-x-auto rounded-3xl bg-slate-950 p-6 text-sm leading-7 text-slate-100">
          <code>{submissionSourceCodeValue(sourceCode)}</code>
        </pre>
      </CardContent>
    </Card>
  )
}
