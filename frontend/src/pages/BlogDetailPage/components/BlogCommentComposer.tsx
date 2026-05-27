import { Alert, AlertDescription } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Textarea } from '@/components/ui/textarea'
import type { useBlogDetailPageModel } from '../hooks/use-blog-detail-page-model'
import { useI18n } from '@/system/i18n/use-i18n'

type BlogDetailPageModel = ReturnType<typeof useBlogDetailPageModel>

type BlogCommentComposerProps = {
  model: BlogDetailPageModel
}

export function BlogCommentComposer({ model }: BlogCommentComposerProps) {
  const { t } = useI18n()

  return (
    <div className="space-y-3 rounded-3xl border border-slate-200 bg-white p-4">
      <Textarea
        value={model.commentContent}
        className="min-h-28"
        onChange={(event) => model.setCommentContent(event.target.value)}
      />
      {model.commentErrorMessage ? (
        <Alert variant="destructive" className="rounded-2xl border-rose-200 bg-rose-50/95">
          <AlertDescription className="text-rose-700">{model.commentErrorMessage}</AlertDescription>
        </Alert>
      ) : null}
      <Button
        type="button"
        disabled={model.isSubmittingComment}
        className="rounded-2xl bg-slate-950 text-white hover:bg-slate-800"
        onClick={() => void model.submitComment()}
      >
        {model.isSubmittingComment ? t('blog.comment.submitting') : t('blog.comment.submit')}
      </Button>
    </div>
  )
}
