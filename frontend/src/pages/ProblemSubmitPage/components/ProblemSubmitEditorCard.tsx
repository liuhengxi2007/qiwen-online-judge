import { FileText, Plus, Send, Trash2, Upload } from 'lucide-react'

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
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs'
import { Textarea } from '@/components/ui/textarea'
import type { SubmissionLanguage } from '@/objects/submission/SubmissionLanguage'
import { isSubmissionLanguage } from '@/objects/submission/SubmissionLanguage'
import { isTextSubmissionRole, type SubmitProgramDraft } from '@/pages/ProblemSubmitPage/functions/SubmitPrograms'
import { useI18n } from '@/system/i18n/use-i18n'

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

/**
 * 单个提交程序编辑器属性，封装角色、语言、源码和文件输入回调。
 */
type ProblemSubmitProgramEditorProps = {
  isOnlyProgram: boolean
  isSubmitting: boolean
  onProgramChange: (id: string, update: Partial<Omit<SubmitProgramDraft, 'id'>>) => void
  onRemoveProgram: (id: string) => void
  program: SubmitProgramDraft
  supportedLanguages: Array<{ value: SubmissionLanguage; label: string }>
}

/**
 * 单个提交程序编辑器，支持粘贴源码或上传源码文件。
 */
function ProblemSubmitProgramEditor({
  isOnlyProgram,
  isSubmitting,
  onProgramChange,
  onRemoveProgram,
  program,
  supportedLanguages,
}: ProblemSubmitProgramEditorProps) {
  // 保留扁平 props：单个程序编辑器字段数量有限，数据和动作都直接作用于同一个 program。
  const { t } = useI18n()
  const isTextRole = isTextSubmissionRole(program.role.trim())
  const selectableLanguages = supportedLanguages.filter((language) => (isTextRole ? language.value === 'text' : language.value !== 'text'))

  return (
    <div className="space-y-4 rounded-lg border border-slate-200 p-4">
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
            disabled={isTextRole}
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
              {selectableLanguages.map((supportedLanguage) => (
                <SelectItem key={supportedLanguage.value} value={supportedLanguage.value}>
                  {supportedLanguage.label}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>
        <Button
          type="button"
          variant="destructiveOutline"
          disabled={isSubmitting || isOnlyProgram}
          className="h-11"
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
        <Tabs
          value={program.sourceMode}
          onValueChange={(value) => {
            if (value === 'paste' || value === 'file') {
              onProgramChange(program.id, { sourceMode: value })
            }
          }}
        >
          <TabsList className="h-9 rounded-lg">
            <TabsTrigger value="paste" className="gap-2 rounded-md">
              <FileText className="size-4" />
              {t('problem.submit.sourcePaste')}
            </TabsTrigger>
            <TabsTrigger value="file" className="gap-2 rounded-md">
              <Upload className="size-4" />
              {t('problem.submit.sourceFile')}
            </TabsTrigger>
          </TabsList>
          <TabsContent value="paste" className="mt-3">
            <Textarea
              id={`problem-submit-source-${program.id}`}
              value={program.sourceCode}
              className="min-h-[22rem] rounded-3xl font-mono text-sm"
              onChange={(event) => {
                onProgramChange(program.id, { sourceCode: event.target.value })
              }}
            />
          </TabsContent>
          <TabsContent value="file" className="mt-3">
            <div className="flex flex-wrap items-center gap-3 rounded-lg border border-dashed border-slate-300 p-4">
            <Input
              id={`problem-submit-file-${program.id}`}
              type="file"
              disabled={isSubmitting}
              className="sr-only"
              onChange={(event) => {
                const file = event.currentTarget.files?.[0]
                event.currentTarget.value = ''
                if (!file) {
                  return
                }
                onProgramChange(program.id, {
                  sourceFile: file,
                  sourceMode: 'file',
                  ...(program.role.trim() ? {} : { role: file.name }),
                })
              }}
            />
            <Button
              asChild
              variant="outline"
              className={`h-9 rounded-lg border-slate-300 bg-white px-3 ${isSubmitting ? 'pointer-events-none opacity-50' : ''}`}
            >
              <label htmlFor={`problem-submit-file-${program.id}`} aria-disabled={isSubmitting}>
                <Upload className="size-4" />
                {t('problem.submit.chooseFile')}
              </label>
            </Button>
              <span className="min-w-0 text-sm text-slate-600">
                {program.sourceFile ? program.sourceFile.name : t('problem.submit.noFileSelected')}
              </span>
              {program.sourceFile ? (
                <Button
                  type="button"
                  variant="ghost"
                  disabled={isSubmitting}
                  className="h-9 rounded-lg px-3"
                  onClick={() => {
                    onProgramChange(program.id, { sourceFile: null })
                  }}
                >
                  {t('problem.submit.clearFile')}
                </Button>
              ) : null}
            </div>
          </TabsContent>
        </Tabs>
          </div>
    </div>
  )
}
