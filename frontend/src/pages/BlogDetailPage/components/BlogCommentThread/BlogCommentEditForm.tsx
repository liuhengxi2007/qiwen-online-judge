import { Button } from '@/components/ui/button'
import { Textarea } from '@/components/ui/textarea'
import type { BlogCommentId } from '@/objects/blog/BlogCommentId'
import { useI18n } from '@/system/i18n/use-i18n'

type BlogCommentEditFormProps = {
  commentId: BlogCommentId
  content: string
  onContentChange: (value: string) => void
  onCancel: () => void
  onSubmit: (commentId: BlogCommentId) => void
}

export function BlogCommentEditForm({
  commentId,
  content,
  onContentChange,
  onCancel,
  onSubmit,
}: BlogCommentEditFormProps) {
  // 保留扁平 props：这是单条评论编辑小表单，字段和动作在调用端具名比再包对象更清楚。
  const { t } = useI18n()

  return (
    <div className="mt-3 space-y-2">
      <Textarea value={content} onChange={(event) => onContentChange(event.target.value)} />
      <div className="flex gap-2">
        <Button size="sm" className="rounded-xl bg-slate-950 text-white" onClick={() => onSubmit(commentId)}>
          {t('common.save')}
        </Button>
        <Button size="sm" variant="outline" className="rounded-xl" onClick={onCancel}>
          {t('common.cancel')}
        </Button>
      </div>
    </div>
  )
}
