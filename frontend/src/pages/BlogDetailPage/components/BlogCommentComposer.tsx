import { Alert, AlertDescription } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Textarea } from '@/components/ui/textarea'
import type { useBlogDetailPageModel } from '../hooks/useBlogDetailPageModel'
import { useI18n } from '@/system/i18n/use-i18n'

/**
 * 博客详情页模型类型别名，供评论输入框访问草稿和提交状态。
 */
type BlogDetailPageModel = ReturnType<typeof useBlogDetailPageModel>

/**
 * 顶层评论输入框属性，传入博客详情页聚合模型。
 */
type BlogCommentComposerProps = {
  model: BlogDetailPageModel
}

/**
 * 顶层评论输入框，展示评论草稿、提交按钮和创建评论错误消息。
 */
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
