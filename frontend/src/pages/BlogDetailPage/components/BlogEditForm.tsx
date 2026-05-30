import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { Textarea } from '@/components/ui/textarea'
import type { useBlogDetailPageModel } from '../hooks/useBlogDetailPageModel'
import type { BlogVisibility } from '@/objects/blog/BlogVisibility'
import { useI18n } from '@/system/i18n/use-i18n'

type BlogDetailPageModel = ReturnType<typeof useBlogDetailPageModel>

type BlogEditFormProps = {
  model: BlogDetailPageModel
}

export function BlogEditForm({ model }: BlogEditFormProps) {
  const { t } = useI18n()

  return (
    <div className="mb-6 space-y-4 rounded-3xl border border-slate-200 bg-slate-50 p-5">
      <Input value={model.editBlogTitle} onChange={(event) => model.setEditBlogTitle(event.target.value)} />
      <Select value={model.editBlogVisibility} onValueChange={(value) => model.setEditBlogVisibility(value as BlogVisibility)}>
        <SelectTrigger className="rounded-2xl border-slate-300 bg-white">
          <SelectValue />
        </SelectTrigger>
        <SelectContent>
          <SelectItem value="public">{t('blog.visibility.public')}</SelectItem>
          <SelectItem value="private">{t('blog.visibility.private')}</SelectItem>
        </SelectContent>
      </Select>
      <Textarea className="min-h-56" value={model.editBlogContent} onChange={(event) => model.setEditBlogContent(event.target.value)} />
      <div className="flex flex-wrap gap-2">
        <Button type="button" className="rounded-2xl bg-slate-950 text-white" onClick={() => void model.submitBlogEdit()}>
          {t('common.save')}
        </Button>
        <Button type="button" variant="outline" className="rounded-2xl border-slate-300 bg-white" onClick={() => model.setIsEditingBlog(false)}>
          {t('common.cancel')}
        </Button>
      </div>
    </div>
  )
}
