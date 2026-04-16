import { Send } from 'lucide-react'

import { Alert, AlertDescription } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Label } from '@/components/ui/label'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'
import { Textarea } from '@/components/ui/textarea'
import type { SubmissionLanguage } from '@/features/submission/domain/submission'
import { isSubmissionLanguage } from '@/features/submission/domain/submission'
import { useI18n } from '@/shared/i18n/i18n'

type ProblemSubmitEditorCardProps = {
  errorMessage: string
  isSubmitting: boolean
  language: SubmissionLanguage
  onLanguageChange: (language: SubmissionLanguage) => void
  onSourceCodeChange: (value: string) => void
  onSubmit: () => void
  sourceCode: string
  statusMessage: string
  supportedLanguages: Array<{ value: SubmissionLanguage; label: string }>
}

export function ProblemSubmitEditorCard({
  errorMessage,
  isSubmitting,
  language,
  onLanguageChange,
  onSourceCodeChange,
  onSubmit,
  sourceCode,
  statusMessage,
  supportedLanguages,
}: ProblemSubmitEditorCardProps) {
  const { t } = useI18n()

  return (
    <Card className="border-slate-200 bg-white shadow-[0_24px_60px_rgba(15,23,42,0.08)]">
      <CardHeader>
        <CardTitle className="text-xl text-slate-950">{t('problem.submit.editorTitle')}</CardTitle>
        <CardDescription>{t('problem.submit.editorDescription')}</CardDescription>
      </CardHeader>
      <CardContent className="space-y-5">
        <div className="space-y-2">
          <Label htmlFor="problem-submit-language">{t('common.languageLabel')}</Label>
          <Select
            value={language}
            onValueChange={(nextLanguage) => {
              if (isSubmissionLanguage(nextLanguage)) {
                onLanguageChange(nextLanguage)
              }
            }}
          >
            <SelectTrigger id="problem-submit-language" className="h-11 rounded-2xl">
              <SelectValue placeholder={t('problem.submit.languagePlaceholder')} />
            </SelectTrigger>
            <SelectContent>
              {supportedLanguages.map((supportedLanguage) => (
                <SelectItem key={supportedLanguage.value} value={supportedLanguage.value}>
                  {supportedLanguage.label}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>

        <div className="space-y-2">
          <Label htmlFor="problem-submit-source">{t('problem.submit.sourceCode')}</Label>
          <Textarea
            id="problem-submit-source"
            value={sourceCode}
            className="min-h-[26rem] rounded-3xl !font-mono text-sm"
            placeholder={t('problem.submit.sourcePlaceholder')}
            onChange={(event) => {
              onSourceCodeChange(event.target.value)
            }}
          />
        </div>

        {errorMessage ? (
          <Alert variant="destructive" className="rounded-2xl border-rose-200 bg-rose-50/95">
            <AlertDescription className="text-rose-700">{errorMessage}</AlertDescription>
          </Alert>
        ) : null}

        {statusMessage ? (
          <Alert className="rounded-2xl border-emerald-200 bg-emerald-50/95">
            <AlertDescription className="text-emerald-700">{statusMessage}</AlertDescription>
          </Alert>
        ) : null}

        <div className="flex flex-wrap gap-3">
          <Button
            type="button"
            disabled={isSubmitting}
            className="rounded-2xl bg-slate-950 text-white hover:bg-slate-800"
            onClick={onSubmit}
          >
            <Send className="size-4" />
            {isSubmitting ? t('problem.submit.submitting') : t('problem.submit.submit')}
          </Button>
        </div>
      </CardContent>
    </Card>
  )
}
