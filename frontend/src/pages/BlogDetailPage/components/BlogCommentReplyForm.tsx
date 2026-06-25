import { Alert, AlertDescription } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Textarea } from '@/components/ui/textarea'
import type { BlogCommentId } from '@/objects/blog/BlogCommentId'
import { useI18n } from '@/system/i18n/use-i18n'

type BlogCommentReplyFormProps = {
  commentId: BlogCommentId
  content: string
  errorMessage: string
  isSubmitting: boolean
  onContentChange: (value: string) => void
  onCancel: () => void
  onSubmit: (commentId: BlogCommentId) => void
}

export function BlogCommentReplyForm({
  commentId,
  content,
  errorMessage,
  isSubmitting,
  onContentChange,
  onCancel,
  onSubmit,
}: BlogCommentReplyFormProps) {
  // 保留扁平 props：回复表单只有一个输入域和提交状态，调用端具名字段足够明确。
  const { t } = useI18n()

  return (
    <div className="mt-4 space-y-3 rounded-2xl border border-slate-200 bg-white p-3">
      <Textarea value={content} className="min-h-24" onChange={(event) => onContentChange(event.target.value)} />
      {errorMessage ? (
        <Alert variant="destructive" className="rounded-2xl border-rose-200 bg-rose-50/95">
          <AlertDescription className="text-rose-700">{errorMessage}</AlertDescription>
        </Alert>
      ) : null}
      <div className="flex flex-wrap gap-2">
        <Button
          type="button"
          size="sm"
          disabled={isSubmitting}
          className="rounded-xl bg-slate-950 text-white hover:bg-slate-800"
          onClick={() => onSubmit(commentId)}
        >
          {isSubmitting ? t('blog.comment.submitting') : t('blog.comment.replySubmit')}
        </Button>
        <Button type="button" size="sm" variant="outline" className="rounded-xl border-slate-300 bg-white" onClick={onCancel}>
          {t('common.cancel')}
        </Button>
      </div>
    </div>
  )
}
