import { FileUp } from 'lucide-react'

import { Alert, AlertDescription } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Card, CardContent } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import type { useProblemDataPageModel } from '../hooks/useProblemDataPageModel'
import { useI18n } from '@/system/i18n/use-i18n'

/**
 * 测试数据页模型类型别名，供上传卡片访问文件选择、提示消息和上传动作。
 */
type ProblemDataPageModel = ReturnType<typeof useProblemDataPageModel>

/**
 * 测试数据上传卡片，接收 zip 或单文件选择并通过页面模型提交上传。
 * 选择文件时会清理旧消息，上传前的覆盖提示由模型根据当前文件树计算。
 */
export function ProblemDataUploadCard({ model }: { model: ProblemDataPageModel }) {
  const { t } = useI18n()

  return (
    <Card className="border-slate-200 bg-white shadow-[0_24px_60px_rgba(15,23,42,0.08)]">
      <CardContent className="space-y-5 pt-6">
        <div className="space-y-2">
          <Label htmlFor="problem-data-file">{t('problem.data.uploadFile')}</Label>
          <Input
            id="problem-data-file"
            type="file"
            className="rounded-2xl"
            onChange={(event) => {
              model.setSelectedFile(event.target.files?.[0] ?? null)
              model.setErrorMessage('')
              model.setSuccessMessage('')
            }}
          />
        </div>

        {model.errorMessage ? (
          <Alert variant="destructive" className="rounded-2xl border-rose-200 bg-rose-50/95">
            <AlertDescription className="text-rose-700">{model.errorMessage}</AlertDescription>
          </Alert>
        ) : null}

        {model.successMessage ? (
          <Alert className="rounded-2xl border-emerald-200 bg-emerald-50/95">
            <AlertDescription className="text-emerald-700">{model.successMessage}</AlertDescription>
          </Alert>
        ) : null}

        {model.uploadWarningMessage ? (
          <Alert className="rounded-2xl border-amber-200 bg-amber-50/95">
            <AlertDescription className="text-amber-900">{model.uploadWarningMessage}</AlertDescription>
          </Alert>
        ) : null}

        <Button
          type="button"
          disabled={model.isUploading || model.selectedFile === null}
          className="rounded-2xl bg-slate-950 text-white hover:bg-slate-800"
          onClick={() => {
            void model.uploadSelectedFile()
          }}
        >
          <FileUp className="size-4" />
          {model.isUploading ? t('problem.data.uploading') : t('problem.data.upload')}
        </Button>
      </CardContent>
    </Card>
  )
}
