import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Textarea } from '@/components/ui/textarea'
import type { useBlogDetailPageModel } from '../hooks/useBlogDetailPageModel'
import { ResourceAccessEditor } from '@/pages/components/ResourceAccessEditor'
import { useI18n } from '@/system/i18n/use-i18n'

/**
 * 博客详情页模型类型别名，供编辑表单读取草稿和提交动作。
 */
type BlogDetailPageModel = ReturnType<typeof useBlogDetailPageModel>

/**
 * 博客编辑表单属性，传入聚合后的详情页模型。
 */
type BlogEditFormProps = {
  model: BlogDetailPageModel
}

/**
 * 博客编辑表单，编辑标题、可见性策略和 Markdown 内容，并由模型负责校验和保存。
 */
export function BlogEditForm({ model }: BlogEditFormProps) {
  const { t } = useI18n()

  return (
    <div className="mb-6 space-y-4 rounded-3xl border border-slate-200 bg-slate-50 p-5">
      <Input value={model.editBlogTitle} onChange={(event) => model.setEditBlogTitle(event.target.value)} />
      <ResourceAccessEditor
        accessPolicy={model.editBlogAccessPolicy}
        grantedUsersInput={model.editBlogGrantedUsersInput}
        grantedGroupsInput={model.editBlogGrantedGroupsInput}
        onBaseAccessChange={model.setEditBlogBaseAccess}
        onGrantedUsersInputChange={model.setEditBlogGrantedUsersInput}
        onGrantedGroupsInputChange={model.setEditBlogGrantedGroupsInput}
      />
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
