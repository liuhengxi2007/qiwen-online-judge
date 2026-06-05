import { Plus, Send, Trash2 } from 'lucide-react'

import { Alert, AlertDescription } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'
import { Textarea } from '@/components/ui/textarea'
import type { SubmissionLanguage } from '@/objects/submission/SubmissionLanguage'
import { isSubmissionLanguage } from '@/objects/submission/SubmissionLanguage'
import { useI18n } from '@/system/i18n/use-i18n'

export type SubmitProgramDraft = {
  id: string
  role: string
  language: SubmissionLanguage
  sourceCode: string
}

type ProblemSubmitEditorCardProps = {
  errorMessage: string
  isSubmitting: boolean
  onAddProgram: () => void
  onProgramChange: (id: string, update: Partial<Omit<SubmitProgramDraft, 'id'>>) => void
  onRemoveProgram: (id: string) => void
  onSubmit: () => void
  programs: SubmitProgramDraft[]
  statusMessage: string
  supportedLanguages: Array<{ value: SubmissionLanguage; label: string }>
}

export function ProblemSubmitEditorCard({
  errorMessage,
  isSubmitting,
  onAddProgram,
  onProgramChange,
  onRemoveProgram,
  onSubmit,
  programs,
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
        <div className="space-y-4">
          {programs.map((program) => (
            <div key={program.id} className="space-y-4 rounded-lg border border-slate-200 p-4">
              <div className="grid gap-3 md:grid-cols-[minmax(10rem,1fr)_minmax(12rem,1fr)_auto] md:items-end">
                <div className="space-y-2">
                  <Label htmlFor={`problem-submit-role-${program.id}`}>Role</Label>
                  <Input
                    id={`problem-submit-role-${program.id}`}
                    value={program.role}
                    className="h-11 rounded-2xl"
                    onChange={(event) => {
                      onProgramChange(program.id, { role: event.target.value })
                    }}
                  />
                </div>
                <div className="space-y-2">
                  <Label htmlFor={`problem-submit-language-${program.id}`}>{t('common.languageLabel')}</Label>
                  <Select
                    value={program.language}
                    onValueChange={(nextLanguage) => {
                      if (isSubmissionLanguage(nextLanguage)) {
                        onProgramChange(program.id, { language: nextLanguage })
                      }
                    }}
                  >
                    <SelectTrigger id={`problem-submit-language-${program.id}`} className="h-11 rounded-2xl">
                      <SelectValue />
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
                <Button
                  type="button"
                  variant="outline"
                  disabled={isSubmitting || programs.length === 1}
                  className="h-11 rounded-2xl border-slate-300 bg-white"
                  onClick={() => {
                    onRemoveProgram(program.id)
                  }}
                >
                  <Trash2 className="size-4" />
                  Remove
                </Button>
              </div>

              <div className="space-y-2">
                <Label htmlFor={`problem-submit-source-${program.id}`}>{t('problem.submit.sourceCode')}</Label>
                <Textarea
                  id={`problem-submit-source-${program.id}`}
                  value={program.sourceCode}
                  className="min-h-[22rem] rounded-3xl font-mono text-sm"
                  onChange={(event) => {
                    onProgramChange(program.id, { sourceCode: event.target.value })
                  }}
                />
              </div>
            </div>
          ))}
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
            variant="outline"
            disabled={isSubmitting}
            className="rounded-2xl border-slate-300 bg-white"
            onClick={onAddProgram}
          >
            <Plus className="size-4" />
            Add role
          </Button>
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
