import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { useI18n } from '@/system/i18n/use-i18n'

type SubmissionJudgeMessageCardProps = {
  judgeMessage: string
}

export function SubmissionJudgeMessageCard({ judgeMessage }: SubmissionJudgeMessageCardProps) {
  const { t } = useI18n()

  return (
    <Card className="border-slate-200 bg-white shadow-[0_24px_60px_rgba(15,23,42,0.08)]">
      <CardHeader>
        <CardTitle className="text-xl text-slate-950">{t('submission.detail.judgeMessage')}</CardTitle>
      </CardHeader>
      <CardContent>
        <p className="whitespace-pre-wrap text-sm leading-7 text-slate-700">
          {judgeMessage}
        </p>
      </CardContent>
    </Card>
  )
}
