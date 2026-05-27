import type { ProblemSlug } from '@/objects/problem/ProblemSlug'
import { problemSlugValue } from '@/objects/problem/problem-parsers'
import { blogIdValue } from '@/objects/blog/blog-parsers'
import type { BlogId } from '@/objects/blog/BlogId'
import { postJson } from '@/system/api/http-client'
import { decodeSuccessResponse } from '@/system/api/http-client'

export async function linkBlogToProblem(problemSlug: ProblemSlug, blogId: BlogId): Promise<void> {
  await postJson(`/api/problems/${problemSlugValue(problemSlug)}/blog-links/${blogIdValue(blogId)}`, decodeSuccessResponse, {})
}
