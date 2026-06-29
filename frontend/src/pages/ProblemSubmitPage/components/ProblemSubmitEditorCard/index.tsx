import { Plus, Send } from 'lucide-react'

import { Alert, AlertDescription } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import type { SubmissionLanguage } from '@/objects/submission/SubmissionLanguage'
import type { SubmitProgramDraft } from '@/pages/ProblemSubmitPage/functions/SubmitPrograms'
import { useI18n } from '@/system/i18n/use-i18n'

import { ProblemSubmitProgramEditor } from './ProblemSubmitProgramEditor'

/**
 * 题目提交编辑卡片属性，包含程序草稿、提交状态和所有字段变更回调。
 */
type ProblemSubmitEditorCardProps = {
  canSubmit: boolean
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

/**
 * 题目提交编辑卡片，渲染多程序源码输入、提交按钮和错误提示。
 */
export function ProblemSubmitEditorCard({
  canSubmit,
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
  // 保留扁平 props：提交编辑器是完整表单边界，程序列表、提交状态和操作回调在调用端并列可读。
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
            <ProblemSubmitProgramEditor
              key={program.id}
              isOnlyProgram={programs.length === 1}
              isSubmitting={isSubmitting}
              onProgramChange={onProgramChange}
              onRemoveProgram={onRemoveProgram}
              program={program}
              supportedLanguages={supportedLanguages}
            />
          ))}
        </div>

        {errorMessage ? (
          <Alert variant="destructive">
            <AlertDescription>{errorMessage}</AlertDescription>
          </Alert>
        ) : null}

        {statusMessage ? (
          <Alert variant="success">
            <AlertDescription>{statusMessage}</AlertDescription>
          </Alert>
        ) : null}

        <div className="flex flex-wrap gap-3">
          <Button
            type="button"
            variant="create"
            disabled={isSubmitting}
            onClick={onAddProgram}
          >
            <Plus className="size-4" />
            Add role
          </Button>
          <Button
            type="button"
            disabled={isSubmitting || !canSubmit}
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
