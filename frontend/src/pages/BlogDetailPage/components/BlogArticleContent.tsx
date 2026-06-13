import { blogContentValue } from '@/objects/blog/BlogContent'
import type { BlogDetail } from '@/objects/blog/response/BlogDetail'
import { MarkdownDocument } from '@/pages/components/MarkdownDocument'

/**
 * 博客正文组件属性，包含已经加载并通过对象类型校验的博客详情。
 */
type BlogArticleContentProps = {
  blog: BlogDetail
}

/**
 * 博客正文渲染组件，将领域对象中的 Markdown 内容交给通用 MarkdownDocument 展示。
 */
export function BlogArticleContent({ blog }: BlogArticleContentProps) {
  return (
    <div className="mt-6 rounded-3xl border border-slate-200 bg-slate-50 px-6 py-6">
      <MarkdownDocument content={blogContentValue(blog.content)} />
    </div>
  )
}
