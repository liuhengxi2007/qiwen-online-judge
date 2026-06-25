import { BlogListPageContent } from './BlogListPageContent'

/**
 * 全站博客列表页，复用博客列表内容组件。
 */
export function BlogPage() {
  // 保留共享博客列表内容组件：这里和 ProblemBlogPage、UserBlogPage 共用筛选、分页和列表逻辑。
  return <BlogListPageContent />
}
