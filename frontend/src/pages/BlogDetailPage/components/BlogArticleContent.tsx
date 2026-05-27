import { blogContentValue } from '@/objects/blog/blog-parsers'
import type { BlogDetail } from '@/objects/blog/response/BlogDetail'
import { MarkdownDocument } from '@/pages/components/markdown-document'

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
