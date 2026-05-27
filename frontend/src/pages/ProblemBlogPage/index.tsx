import { Navigate, useParams } from 'react-router-dom'

import { parseProblemSlug } from '@/objects/problem/problem-parsers'
import { BlogListPageContent } from '@/pages/BlogPage/BlogListPageContent'

export function ProblemBlogPage() {
  const { slug } = useParams<{ slug: string }>()
  const slugResult = parseProblemSlug(slug ?? '')

  if (!slugResult.ok) {
    return <Navigate replace to="/blogs" />
  }

  return <BlogListPageContent problemSlugFilter={slugResult.value} />
}
