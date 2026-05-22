import type { ProblemSlug } from '@/features/problem/domain/problem'
import { problemSlugValue } from '@/features/problem/domain/problem'
import { blogIdValue } from '@/features/blog/domain/blog'
import type { BlogId } from '@/features/blog/domain/blog'
import { postJson } from '@/shared/api/http-client'
import { decodeSuccessResponse } from '@/shared/api/http-client'

export async function unlinkBlogFromProblem(problemSlug: ProblemSlug, blogId: BlogId): Promise<void> {
  await postJson(`/api/problems/${problemSlugValue(problemSlug)}/blog-links/${blogIdValue(blogId)}/delete`, decodeSuccessResponse, {})
}
