import { Navigate, useParams } from 'react-router-dom'

import { parseUsername } from '@/objects/user/user-parsers'
import { BlogListPageContent } from '@/pages/BlogPage/BlogListPageContent'

export function UserBlogPage() {
  const { username } = useParams<{ username: string }>()
  const usernameResult = parseUsername(username ?? '')

  if (!usernameResult.ok) {
    return <Navigate replace to="/blogs" />
  }

  return <BlogListPageContent authorUsernameFilter={usernameResult.value} />
}
