import { blogContentValue } from '@/features/blog/lib/blog-parsers'
import type { BlogDetail } from '@/features/blog/model/response/BlogDetail'
import { MarkdownDocument } from '@/shared/components/markdown-document'

type BlogArticleContentProps = {
  blog: BlogDetail
}

export function BlogArticleContent({ blog }: BlogArticleContentProps) {
  return (
    <div className="mt-6 rounded-3xl border border-slate-200 bg-slate-50 px-6 py-6">
      <MarkdownDocument content={blogContentValue(blog.content)} />
    </div>
  )
}
