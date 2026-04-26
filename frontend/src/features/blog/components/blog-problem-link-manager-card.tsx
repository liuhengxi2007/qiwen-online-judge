import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { useI18n } from '@/shared/i18n/i18n'

type BlogProblemLinkManagerCardProps = {
  linkBlogId: string
  linkMessage: string
  isLinking: boolean
  onLinkBlogIdChange: (value: string) => void
  onSubmit: () => void
}

export function BlogProblemLinkManagerCard({
  linkBlogId,
  linkMessage,
  isLinking,
  onLinkBlogIdChange,
  onSubmit,
}: BlogProblemLinkManagerCardProps) {
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
          className="rounded-2xl bg-orange-300 text-orange-950 hover:bg-orange-400"
          onClick={onSubmit}
        >
          {isLinking ? t('common.loading') : t('blog.problem.linkSubmit')}
        </Button>
      </div>
      {linkMessage ? <p className="mt-2 text-sm text-orange-800">{linkMessage}</p> : null}
    </div>
  )
}
