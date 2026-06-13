import { Link, Navigate, useParams } from 'react-router-dom'

import { Button } from '@/components/ui/button'
import { CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs'
import { Textarea } from '@/components/ui/textarea'
import { FileText, Upload } from 'lucide-react'
import type { HackDetail } from '@/objects/hack/response/HackDetail'
import { hackIdValue } from '@/objects/hack/HackId'
import { parseSubmissionId, submissionIdValue } from '@/objects/submission/SubmissionId'
import type { SubmissionId } from '@/objects/submission/SubmissionId'
import { HackCard, HackErrorAlert } from '@/pages/components/HackCard'
import { HackMetric } from '@/pages/components/HackMetric'
import { PageLoadingCard } from '@/pages/components/PageLoadingCard'
import { PageShell } from '@/pages/components/PageShell'
import { UserProfileLink } from '@/pages/components/UserProfileLink'
import { usePageTitle } from '@/pages/hooks/usePageTitle'
import { useSessionGuard } from '@/pages/hooks/useSessionGuard'
import { hackModeLabel, hackStatusLabel } from '@/pages/objects/HackDisplay'
import { formatOptionalScore } from '@/pages/objects/SubmissionDisplay'
import type { HackSourceMode } from './functions/HackSourceMode'
import { useSubmissionHackModel } from './hooks/useSubmissionHackModel'
import { useI18n } from '@/system/i18n/use-i18n'

/**
 * 提交 Hack 页面入口，校验路由参数和会话后进入具体 Hack 表单内容。
 */
export function SubmissionHackPage() {
  const { t } = useI18n()
  usePageTitle(t('hack.submit.pageTitle'))
  const { session: user, navigationIntent } = useSessionGuard()
  const { submissionId, subtaskIndex } = useParams<{ submissionId: string; subtaskIndex: string }>()

  if (navigationIntent) {
    return <Navigate replace={navigationIntent.replace} to={navigationIntent.to} />
  }

  if (!user) {
    return <Navigate replace to="/login" />
  }

  const submissionIdResult = parseSubmissionId(Number(submissionId ?? ''))
  const parsedSubtaskIndex = Number(subtaskIndex ?? '')
  if (!submissionIdResult.ok || !Number.isSafeInteger(parsedSubtaskIndex) || parsedSubtaskIndex < 1) {
    return <Navigate replace to="/submissions" />
  }

  return <SubmissionHackPageContent submissionId={submissionIdResult.value} subtaskIndex={parsedSubtaskIndex} />
}

/**
 * 提交 Hack 页面主体，加载目标子任务信息并渲染输入、策略生成器和 Hack 结果。
 */
function SubmissionHackPageContent({ submissionId, subtaskIndex }: { submissionId: SubmissionId; subtaskIndex: number }) {
  const { t } = useI18n()
  const model = useSubmissionHackModel({ submissionId, subtaskIndex })

  return (
    <PageShell title={t('hack.submit.heading')} mainClassName="bg-[linear-gradient(180deg,#f8fafc_0%,#edf4fb_100%)]">
      {model.errorMessage ? <HackErrorAlert message={model.errorMessage} /> : null}

      {model.isLoading ? (
        <PageLoadingCard message={t('hack.submit.loading')} />
      ) : model.info ? (
        <div className="space-y-6">
          <HackCard>
            <CardHeader>
              <CardTitle className="text-xl text-slate-950">{t('hack.submit.target')}</CardTitle>
            </CardHeader>
            <CardContent className="grid gap-3 text-sm text-slate-600 sm:grid-cols-3">
              <HackMetric label={t('submission.list.id')} value={`#${submissionIdValue(model.info.targetSubmissionId)}`} />
              <HackMetric label={t('submission.list.submitter')} value={<UserProfileLink user={model.info.targetSubmitter} />} />
              <HackMetric label={t('hack.subtask')} value={model.info.subtaskLabel ? `${model.info.subtaskIndex} (${model.info.subtaskLabel})` : String(model.info.subtaskIndex)} />
              <HackMetric label={t('hack.subtaskScore')} value={formatOptionalScore(model.info.oldWorstScore)} />
              <HackMetric label={t('hack.mode')} value={hackModeLabel(model.info.mode, t)} />
            </CardContent>
          </HackCard>

          <HackCard>
            <CardHeader>
              <CardTitle className="text-xl text-slate-950">{t('hack.submit.input')}</CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="space-y-2">
                <Label htmlFor="hack-input">{t('hack.submit.input')}</Label>
                <HackSourceTabs
                  file={model.inputFile}
                  fileInputId="hack-input-file"
                  mode={model.inputMode}
                  onFileChange={model.setInputFile}
                  onModeChange={model.setInputMode}
                  onTextChange={model.setInput}
                  text={model.input}
                  textAreaId="hack-input"
                  disabled={model.isSubmitting}
                />
              </div>
              {model.info.requiresStrategyProvider ? (
                <div className="space-y-2">
                  <Label htmlFor="hack-strategy">{t('hack.submit.strategyProvider')}</Label>
                  <HackSourceTabs
                    file={model.strategyProviderFile}
                    fileInputId="hack-strategy-file"
                    mode={model.strategyProviderMode}
                    onFileChange={model.setStrategyProviderFile}
                    onModeChange={model.setStrategyProviderMode}
                    onTextChange={model.setStrategyProviderSource}
                    text={model.strategyProviderSource}
                    textAreaId="hack-strategy"
                    disabled={model.isSubmitting}
                  />
                </div>
              ) : null}
              <Button disabled={model.isSubmitting || Boolean(model.hack) || !model.canSubmit} onClick={model.submit}>
                {model.isSubmitting ? t('hack.submit.submitting') : t('hack.submit.action')}
              </Button>
            </CardContent>
          </HackCard>

          {model.hack ? <HackAttemptPanel hack={model.hack} /> : null}
        </div>
      ) : null}
    </PageShell>
  )
}

/**
 * Hack 数据源标签页属性，封装文本/文件模式的值和变更回调。
 */
type HackSourceTabsProps = {
  disabled: boolean
  file: File | null
  fileInputId: string
  mode: HackSourceMode
  onFileChange: (file: File | null) => void
  onModeChange: (mode: HackSourceMode) => void
  onTextChange: (value: string) => void
  text: string
  textAreaId: string
}

/**
 * Hack 数据源输入组件，允许在粘贴文本和选择文件之间切换。
 */
function HackSourceTabs({
  disabled,
  file,
  fileInputId,
  mode,
  onFileChange,
  onModeChange,
  onTextChange,
  text,
  textAreaId,
}: HackSourceTabsProps) {
  const { t } = useI18n()

  return (
    <Tabs
      value={mode}
      onValueChange={(value) => {
        if (value === 'paste' || value === 'file') {
          onModeChange(value)
        }
      }}
    >
      <TabsList className="h-9 rounded-lg">
        <TabsTrigger value="paste" className="gap-2 rounded-md">
          <FileText className="size-4" />
          {t('hack.submit.sourcePaste')}
        </TabsTrigger>
        <TabsTrigger value="file" className="gap-2 rounded-md">
          <Upload className="size-4" />
          {t('hack.submit.sourceFile')}
        </TabsTrigger>
      </TabsList>
      <TabsContent value="paste" className="mt-3">
        <Textarea id={textAreaId} className="min-h-48 font-mono" value={text} onChange={(event) => onTextChange(event.target.value)} />
      </TabsContent>
      <TabsContent value="file" className="mt-3">
        <div className="flex flex-wrap items-center gap-3 rounded-lg border border-dashed border-slate-300 p-4">
          <Input
            id={fileInputId}
            type="file"
            disabled={disabled}
            className="sr-only"
            onChange={(event) => {
              const selectedFile = event.currentTarget.files?.[0] ?? null
              event.currentTarget.value = ''
              onFileChange(selectedFile)
            }}
          />
          <Button
            asChild
            variant="outline"
            className={`h-9 rounded-lg border-slate-300 bg-white px-3 ${disabled ? 'pointer-events-none opacity-50' : ''}`}
          >
            <label htmlFor={fileInputId} aria-disabled={disabled}>
              <Upload className="size-4" />
              {t('hack.submit.chooseFile')}
            </label>
          </Button>
          <span className="min-w-0 text-sm text-slate-600">{file ? file.name : t('hack.submit.noFileSelected')}</span>
          {file ? (
            <Button type="button" variant="ghost" disabled={disabled} className="h-9 rounded-lg px-3" onClick={() => onFileChange(null)}>
              {t('hack.submit.clearFile')}
            </Button>
          ) : null}
        </div>
      </TabsContent>
    </Tabs>
  )
}

/**
 * Hack 创建后的结果面板，展示 Hack 编号、状态、分数变化和各阶段输出消息。
 */
function HackAttemptPanel({ hack }: { hack: HackDetail }) {
  const { t } = useI18n()
  return (
    <HackCard>
      <CardHeader>
        <CardTitle className="text-xl text-slate-950">
          <Link className="hover:underline" to={`/hacks/${hackIdValue(hack.id)}`}>#{hackIdValue(hack.id)}</Link>
        </CardTitle>
      </CardHeader>
      <CardContent className="grid gap-3 text-sm text-slate-600 sm:grid-cols-3">
        <HackMetric label={t('hack.status')} value={hackStatusLabel(hack.status, t)} />
        <HackMetric label={t('hack.oldScore')} value={formatOptionalScore(hack.oldScore)} />
        <HackMetric label={t('hack.newScore')} value={formatOptionalScore(hack.newScore)} />
        <HackMetric label={t('hack.validator')} value={hack.validatorMessage ?? '--'} />
        <HackMetric label={t('hack.standard')} value={hack.standardMessage ?? '--'} />
        <HackMetric label={t('hack.targetRun')} value={hack.targetMessage ?? '--'} />
      </CardContent>
    </HackCard>
  )
}
