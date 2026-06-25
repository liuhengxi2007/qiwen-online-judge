import { Navigate, useParams } from 'react-router-dom'

import { parseProblemSlug } from '@/objects/problem/ProblemSlug'
import { BlogListPageContent } from '@/pages/BlogPage/BlogListPageContent'

/**
 * 题目关联博客列表路由页，解析题目 slug 并复用通用博客列表的题目过滤能力。
 * 非法 slug 会重定向到全站博客页，避免用错误参数触发后续查询。
 */
export function ProblemBlogPage() {
  const { slug } = useParams<{ slug: string }>()
  const slugResult = parseProblemSlug(slug ?? '')

  if (!slugResult.ok) {
    return <Navigate replace to="/blogs" />
  }

  // 保留共享博客列表内容组件：这里和 BlogPage、UserBlogPage 共用筛选、分页和列表逻辑。
  return <BlogListPageContent problemSlugFilter={slugResult.value} />
}
