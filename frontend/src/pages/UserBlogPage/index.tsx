import { Navigate, useParams } from 'react-router-dom'

import { parseUsername } from '@/objects/user/Username'
import { BlogListPageContent } from '@/pages/BlogPage/BlogListPageContent'

/**
 * 用户博客列表路由页，解析 URL 中的用户名并把合法用户名作为作者过滤条件传给通用博客列表。
 * 用户名非法时回退到全站博客列表，不在前端泄漏不存在或非法用户的额外状态。
 */
export function UserBlogPage() {
  const { username } = useParams<{ username: string }>()
  const usernameResult = parseUsername(username ?? '')

  if (!usernameResult.ok) {
    return <Navigate replace to="/blogs" />
  }

  // 保留共享博客列表内容组件：这里和 BlogPage、ProblemBlogPage 共用筛选、分页和列表逻辑。
  return <BlogListPageContent authorUsernameFilter={usernameResult.value} />
}
