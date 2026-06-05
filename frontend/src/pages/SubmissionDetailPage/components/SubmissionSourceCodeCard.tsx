import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs'
import { submissionSourceCodeValue } from '@/objects/submission/SubmissionSourceCode'
import type { SubmissionSourceCode } from '@/objects/submission/SubmissionSourceCode'
import type { SubmissionDetail } from '@/objects/submission/response/SubmissionDetail'
import { useI18n } from '@/system/i18n/use-i18n'

type SubmissionDetailProgram = SubmissionDetail['programs'][string]

type SubmissionSourceCodeCardProps = {
  sourceCode: SubmissionSourceCode
  programs: Record<string, SubmissionDetailProgram>
}

export function SubmissionSourceCodeCard({ programs, sourceCode }: SubmissionSourceCodeCardProps) {
  const { t } = useI18n()
  const entries = Object.entries(programs) as Array<[string, SubmissionDetailProgram]>
  const fallbackEntries: Array<[string, SubmissionDetailProgram]> = [['main', { language: 'cpp17', sourceCode }]]
  const sourceEntries = entries.length > 0 ? entries : fallbackEntries
  const defaultRole = sourceEntries[0]?.[0] ?? 'main'

  return (
    <Card className="border-slate-200 bg-white shadow-[0_24px_60px_rgba(15,23,42,0.08)]">
      <CardHeader>
        <CardTitle className="text-xl text-slate-950">{t('submission.detail.sourceCode')}</CardTitle>
        <CardDescription>{t('submission.detail.sourceDescription')}</CardDescription>
      </CardHeader>
      <CardContent>
        <Tabs defaultValue={defaultRole} className="space-y-4">
          <TabsList className="flex h-auto flex-wrap justify-start rounded-lg">
            {sourceEntries.map(([role]) => (
              <TabsTrigger key={role} value={role} className="rounded-md">
                {role}
              </TabsTrigger>
            ))}
          </TabsList>
          {sourceEntries.map(([role, program]) => (
            <TabsContent key={role} value={role}>
              <pre className="overflow-x-auto rounded-3xl bg-slate-950 p-6 text-sm leading-7 text-slate-100">
                <code>{submissionSourceCodeValue(program.sourceCode)}</code>
              </pre>
            </TabsContent>
          ))}
        </Tabs>
      </CardContent>
    </Card>
  )
}
