import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { useI18n } from '@/system/i18n/use-i18n'

/**
 * 博客题目关联管理卡片属性，包含待处理数量。
 */
type BlogProblemLinkManagerCardProps = {
  linkBlogId: string
  linkMessage: string
  isLinking: boolean
  onLinkBlogIdChange: (value: string) => void
  onSubmit: () => void
}

/**
 * 博客题目关联管理卡片，提示用户处理未关联的题解。
 */
export function BlogProblemLinkManagerCard({
  linkBlogId,
  linkMessage,
  isLinking,
  onLinkBlogIdChange,
  onSubmit,
}: BlogProblemLinkManagerCardProps) {
  // 保留扁平 props：这是单一关联操作卡片，输入值、状态和提交动作并列展示更直观。
  const { t } = useI18n()

  return (
    <div className="rounded-3xl border border-orange-100 bg-orange-50 p-4">
      <div className="flex flex-col gap-3 sm:flex-row sm:items-end">
        <div className="space-y-2 sm:max-w-xs">
          <label className="text-sm font-medium text-orange-950" htmlFor="blog-link-id">
            {t('blog.problem.linkExisting')}
          </label>
          <Input id="blog-link-id" value={linkBlogId} onChange={(event) => onLinkBlogIdChange(event.target.value)} />
        </div>
        <Button
          type="button"
          disabled={isLinking}
          variant="create"
          onClick={onSubmit}
        >
          {isLinking ? t('common.loading') : t('blog.problem.linkSubmit')}
        </Button>
      </div>
      {linkMessage ? <p className="mt-2 text-sm text-orange-800">{linkMessage}</p> : null}
    </div>
  )
}
