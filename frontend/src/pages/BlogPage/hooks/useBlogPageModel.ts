import type { ProblemSlug } from '@/objects/problem/ProblemSlug'
import type { PageRequest } from '@/objects/shared/PageRequest'
import type { Username } from '@/objects/user/Username'
import { useBlogListQuery } from './useBlogListQuery'
import { useProblemBlogManagement } from './useProblemBlogManagement'

type UseBlogPageModelArgs = {
  authorUsernameFilter?: Username
  problemSlugFilter?: ProblemSlug
  canManageProblemLinks: boolean
  pageRequest: PageRequest
}

export function useBlogPageModel({
  authorUsernameFilter,
  problemSlugFilter,
  canManageProblemLinks,
  pageRequest,
}: UseBlogPageModelArgs) {
  const blogList = useBlogListQuery(authorUsernameFilter ?? null, problemSlugFilter ?? null, pageRequest)
  const problemBlogManagement = useProblemBlogManagement({
    canManageProblemLinks,
    problemSlugFilter,
    reloadBlogs: blogList.reload,
  })

  return {
    ...blogList,
    ...problemBlogManagement,
  }
}
