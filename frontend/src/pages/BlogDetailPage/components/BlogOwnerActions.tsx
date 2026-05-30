import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import type { useBlogDetailPageModel } from '../hooks/useBlogDetailPageModel'
import type { BlogDetail } from '@/objects/blog/response/BlogDetail'
import { useI18n } from '@/system/i18n/use-i18n'

type BlogDetailPageModel = ReturnType<typeof useBlogDetailPageModel>

type BlogOwnerActionsProps = {
  blog: BlogDetail
  model: BlogDetailPageModel
}

export function BlogOwnerActions({ blog, model }: BlogOwnerActionsProps) {
  const { t } = useI18n()

  return (
    <div className="mb-5 space-y-4">
      <div className="flex flex-wrap gap-2">
        <Button type="button" variant="outline" className="rounded-2xl border-slate-300 bg-white" onClick={model.startEditingBlog}>
          {t('common.edit')}
        </Button>
        <Button type="button" variant="outline" className="rounded-2xl border-rose-200 bg-white text-rose-700" onClick={() => void model.removeBlog()}>
          {t('common.delete')}
        </Button>
      </div>
      {blog.visibility === 'public' ? (
        <div className="rounded-3xl border border-orange-100 bg-orange-50 p-4">
          <div className="flex flex-col gap-3 sm:flex-row sm:items-end">
            <div className="space-y-2 sm:max-w-xs">
              <label className="text-sm font-medium text-orange-950" htmlFor="submit-blog-problem-slug">
                {t('blog.problem.submitToProblem')}
              </label>
              <Input
                id="submit-blog-problem-slug"
                value={model.submitProblemSlug}
                onChange={(event) => model.setSubmitProblemSlug(event.target.value)}
              />
            </div>
            <Button
              type="button"
              disabled={model.isSubmittingToProblem}
              className="rounded-2xl bg-orange-300 text-orange-950 hover:bg-orange-400"
              onClick={() => void model.submitToProblem()}
            >
              {model.isSubmittingToProblem ? t('common.loading') : t('blog.problem.submit')}
            </Button>
          </div>
          {model.submitProblemMessage ? <p className="mt-2 text-sm text-orange-800">{model.submitProblemMessage}</p> : null}
        </div>
      ) : null}
    </div>
  )
}
