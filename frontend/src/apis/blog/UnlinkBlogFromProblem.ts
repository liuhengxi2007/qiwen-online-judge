import type { ProblemSlug } from '@/objects/problem/ProblemSlug'
import { problemSlugValue } from '@/objects/problem/ProblemSlug'
import { blogIdValue } from '@/objects/blog/BlogId'
import type { BlogId } from '@/objects/blog/BlogId'
import { postJson } from '@/system/api/http-client'
import { decodeSuccessResponse } from '@/system/api/http-client'

export async function unlinkBlogFromProblem(problemSlug: ProblemSlug, blogId: BlogId): Promise<void> {
  await postJson(`/api/problems/${problemSlugValue(problemSlug)}/blog-links/${blogIdValue(blogId)}/delete`, decodeSuccessResponse, {})
}
