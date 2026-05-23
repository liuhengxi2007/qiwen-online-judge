import type { ProblemSlug } from '@/features/problem/model/ProblemSlug'
import { problemSlugValue } from '@/features/problem/lib/problem-parsers'
import { blogIdValue } from '@/features/blog/lib/blog-parsers'
import type { BlogId } from '@/features/blog/model/BlogId'
import { postJson } from '@/shared/api/http-client'
import { decodeSuccessResponse } from '@/shared/api/http-client'

export async function unlinkBlogFromProblem(problemSlug: ProblemSlug, blogId: BlogId): Promise<void> {
  await postJson(`/api/problems/${problemSlugValue(problemSlug)}/blog-links/${blogIdValue(blogId)}/delete`, decodeSuccessResponse, {})
}
