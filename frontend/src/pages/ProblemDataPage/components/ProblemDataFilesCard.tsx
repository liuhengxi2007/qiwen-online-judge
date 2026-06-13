import { Archive, ArrowDownToLine, Eraser, FolderTree, Trash2 } from 'lucide-react'

import { Button } from '@/components/ui/button'
import { Card, CardContent } from '@/components/ui/card'
import { problemDataPathValue } from '@/objects/problem/ProblemDataPath'
import { ConfirmActionDialog } from '@/pages/components/ConfirmActionDialog'
import { formatOptionalBinarySizeBytes } from '@/system/format/binary-size'
import { useI18n } from '@/system/i18n/use-i18n'
import type { useProblemDataPageModel } from '../hooks/useProblemDataPageModel'

/**
 * 测试数据页模型类型别名，限定文件列表卡片只消费页面模型的公开字段和动作。
 */
type ProblemDataPageModel = ReturnType<typeof useProblemDataPageModel>

/**
 * 测试数据文件列表卡片属性，传入聚合后的页面模型。
 */
type ProblemDataFilesCardProps = {
  model: ProblemDataPageModel
}

/**
 * 测试数据文件列表卡片，展示目录树、文件大小、下载链接以及删除/清空确认入口。
 * 清空和删除操作通过模型回调触发 API，组件本身只负责展示 loading/禁用状态。
 */
export function ProblemDataFilesCard({ model }: ProblemDataFilesCardProps) {
  const { t } = useI18n()
  const hasFileNodes = model.dataTree.some((node) => node.kind === 'file')

  return (
    <Card className="border-slate-200 bg-white shadow-[0_24px_60px_rgba(15,23,42,0.08)]">
      <CardContent className="space-y-3 pt-6">
        <div className="space-y-3 rounded-3xl border border-slate-200 bg-slate-50 px-5 py-5">
          <div>
            <p className="text-sm font-medium text-slate-900">{t('problem.data.filesTitle')}</p>
            <p className="mt-1 text-sm text-slate-500">{t('problem.data.filesDescription')}</p>
          </div>

          {model.isLoadingFiles ? (
            <p className="text-sm text-slate-500">{t('problem.data.loadingFiles')}</p>
          ) : model.dataTree.length === 0 ? (
            <p className="text-sm text-slate-500">{t('problem.data.emptyFiles')}</p>
          ) : (
            <div className="space-y-3">
              <div className="flex flex-col justify-end gap-3 sm:flex-row">
                {hasFileNodes ? (
                  <Button asChild variant="outline" className="rounded-2xl border-slate-300 bg-white">
                    <a href={model.downloadDataArchiveUrl()}>
                      <Archive className="size-4" />
                      {t('problem.data.downloadZip')}
                    </a>
                  </Button>
                ) : null}
                <ConfirmActionDialog
                  title={t('problem.data.clearAllTitle')}
                  description={t('problem.data.clearAllDescription')}
                  confirmLabel={model.isClearingAll ? t('problem.data.clearing') : t('problem.data.clearAll')}
                  destructive
                  onConfirm={() => {
                    void model.clearAllDataFiles()
                  }}
                  trigger={
                    <Button
                      type="button"
                      variant="outline"
                      disabled={model.isClearingAll}
                      className="rounded-2xl border-rose-200 bg-white text-rose-700 hover:bg-rose-50 hover:text-rose-800"
                    >
                      <Eraser className="size-4" />
                      {model.isClearingAll ? t('problem.data.clearing') : t('problem.data.clearAll')}
                    </Button>
                  }
                />
              </div>
              {model.dataTree.map((node) => {
                const path = problemDataPathValue(node.path)
                return (
                  <div
                    key={path}
                    className="flex flex-col gap-3 rounded-2xl border border-slate-200 bg-white px-4 py-4 sm:flex-row sm:items-center sm:justify-between"
                    style={{ paddingLeft: `${16 + (path.split('/').length - 1) * 20}px` }}
                  >
                    <div className="flex min-w-0 items-center gap-2">
                      {node.kind === 'directory' ? <FolderTree className="size-4 shrink-0 text-slate-500" /> : null}
                      <div className="min-w-0">
                        <p className="break-all text-sm font-medium text-slate-900">{path}</p>
                        {node.kind === 'file' ? (
                          <p className="mt-1 text-xs text-slate-500">
                            {formatOptionalBinarySizeBytes(node.sizeBytes, t('problem.data.sizeUnknown'))}
                          </p>
                        ) : null}
                      </div>
                    </div>
                    {node.kind === 'file' ? (
                      <div className="flex flex-col gap-3 sm:flex-row">
                        <Button asChild variant="outline" className="rounded-2xl border-slate-300 bg-white">
                          <a href={model.downloadDataPathUrl(node.path)}>
                            <ArrowDownToLine className="size-4" />
                            {t('problem.data.download')}
                          </a>
                        </Button>
                        <Button
                          type="button"
                          variant="outline"
                          disabled={model.deletingPath === node.path}
                          className="rounded-2xl border-rose-200 bg-rose-50 text-rose-700 hover:bg-rose-100 hover:text-rose-800"
                          onClick={() => {
                            void model.deleteDataPath(node.path)
                          }}
                        >
                          <Trash2 className="size-4" />
                          {t('problem.data.delete')}
                        </Button>
                      </div>
                    ) : null}
                  </div>
                )
              })}
            </div>
          )}
        </div>
      </CardContent>
    </Card>
  )
}
