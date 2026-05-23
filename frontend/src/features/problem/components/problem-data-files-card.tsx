import { ArrowDownToLine, Eraser, FolderTree, Trash2 } from 'lucide-react'

import { Button } from '@/components/ui/button'
import { Card, CardContent } from '@/components/ui/card'
import { problemDataPathDownloadUrl } from '@/features/problem/http/api/problem-client'
import { problemDataPathValue } from '@/features/problem/lib/problem-parsers'
import type { ProblemSlug } from '@/features/problem/model/ProblemSlug'
import type { useProblemDataPageModel } from '@/features/problem/hooks/use-problem-data-page-model'
import { ConfirmActionDialog } from '@/shared/components/confirm-action-dialog'
import { useI18n } from '@/shared/i18n/use-i18n'

type ProblemDataPageModel = ReturnType<typeof useProblemDataPageModel>

type ProblemDataFilesCardProps = {
  model: ProblemDataPageModel
  problemSlug: ProblemSlug
}

export function ProblemDataFilesCard({ model, problemSlug }: ProblemDataFilesCardProps) {
  const { t } = useI18n()

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
              <div className="flex justify-end">
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
              {model.dataTree.map((node) => (
                <div
                  key={problemDataPathValue(node.path)}
                  className="flex flex-col gap-3 rounded-2xl border border-slate-200 bg-white px-4 py-4 sm:flex-row sm:items-center sm:justify-between"
                  style={{ paddingLeft: `${16 + (problemDataPathValue(node.path).split('/').length - 1) * 20}px` }}
                >
                  <div className="flex items-center gap-2">
                    {node.kind === 'directory' ? <FolderTree className="size-4 text-slate-500" /> : null}
                    <p className="text-sm font-medium text-slate-900">{problemDataPathValue(node.path)}</p>
                  </div>
                  {node.kind === 'file' ? (
                    <div className="flex flex-col gap-3 sm:flex-row">
                      <Button asChild variant="outline" className="rounded-2xl border-slate-300 bg-white">
                        <a href={problemDataPathDownloadUrl(problemSlug, node.path)}>
                          <ArrowDownToLine className="size-4" />
                          {t('problem.data.download')}
                        </a>
                      </Button>
                      <Button
                        type="button"
                        variant="outline"
                        disabled={model.deletingFilename === (node.path.split('/').slice(-1)[0] as never)}
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
              ))}
            </div>
          )}
        </div>
      </CardContent>
    </Card>
  )
}
