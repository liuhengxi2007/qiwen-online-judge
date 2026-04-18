import type { BlogCommentId, BlogDetail, BlogListResponse, BlogSummary, CreateBlogCommentRequest, CreateBlogRequest, UpdateBlogCommentRequest, UpdateBlogRequest, VoteBlogCommentRequest, VoteBlogRequest } from '@/features/blog/domain/blog'
import type { Username } from '@/features/auth/domain/auth'
import { usernameValue } from '@/features/auth/domain/auth'
import type { ProblemSlug } from '@/features/problem/domain/problem'
import { problemSlugValue } from '@/features/problem/domain/problem'
import {
  blogCommentIdValue,
  blogIdValue,
  fromBlogDetailContract,
  fromBlogListResponseContract,
  fromBlogSummaryContract,
  toCreateBlogCommentRequestContract,
  toCreateBlogRequestContract,
  toUpdateBlogCommentRequestContract,
  toUpdateBlogRequestContract,
  toVoteBlogCommentRequestContract,
  toVoteBlogRequestContract,
} from '@/features/blog/domain/blog'
import type { BlogId } from '@/features/blog/domain/blog'
import { postJson, requestJson } from '@/shared/api/http-client'
import { decodeSuccessResponse } from '@/shared/api/http-client'

export async function listBlogs(authorUsername?: Username | null): Promise<BlogListResponse> {
  const url = new URL('/api/blogs', window.location.origin)
  if (authorUsername) {
    url.searchParams.set('username', usernameValue(authorUsername))
  }

  return requestJson(url.pathname + url.search, fromBlogListResponseContract)
}

export async function listProblemBlogs(problemSlug: ProblemSlug): Promise<BlogListResponse> {
  return requestJson(`/api/problems/${problemSlugValue(problemSlug)}/blogs`, fromBlogListResponseContract)
}

export async function createBlog(request: CreateBlogRequest): Promise<BlogSummary> {
  return postJson('/api/blogs', fromBlogSummaryContract, toCreateBlogRequestContract(request))
}

export async function getBlog(blogId: BlogId): Promise<BlogDetail> {
  return requestJson(`/api/blogs/${blogIdValue(blogId)}`, fromBlogDetailContract)
}

export async function voteBlog(blogId: BlogId, request: VoteBlogRequest): Promise<BlogDetail> {
  return postJson(`/api/blogs/${blogIdValue(blogId)}/vote`, fromBlogDetailContract, toVoteBlogRequestContract(request))
}

export async function updateBlog(blogId: BlogId, request: UpdateBlogRequest): Promise<BlogDetail> {
  return postJson(`/api/blogs/${blogIdValue(blogId)}/update`, fromBlogDetailContract, toUpdateBlogRequestContract(request))
}

export async function deleteBlog(blogId: BlogId): Promise<void> {
  await postJson(`/api/blogs/${blogIdValue(blogId)}/delete`, decodeSuccessResponse, {})
}

export async function createBlogComment(
  blogId: BlogId,
  request: CreateBlogCommentRequest,
  parentCommentId?: BlogCommentId | null,
): Promise<BlogDetail> {
  const path = parentCommentId
    ? `/api/blogs/${blogIdValue(blogId)}/comments/${blogCommentIdValue(parentCommentId)}/replies`
    : `/api/blogs/${blogIdValue(blogId)}/comments`
  return postJson(path, fromBlogDetailContract, toCreateBlogCommentRequestContract(request))
}

export async function voteBlogComment(
  blogId: BlogId,
  commentId: BlogCommentId,
  request: VoteBlogCommentRequest,
): Promise<BlogDetail> {
  return postJson(
    `/api/blogs/${blogIdValue(blogId)}/comments/${blogCommentIdValue(commentId)}/vote`,
    fromBlogDetailContract,
    toVoteBlogCommentRequestContract(request),
  )
}

export async function updateBlogComment(
  blogId: BlogId,
  commentId: BlogCommentId,
  request: UpdateBlogCommentRequest,
): Promise<BlogDetail> {
  return postJson(
    `/api/blogs/${blogIdValue(blogId)}/comments/${blogCommentIdValue(commentId)}/update`,
    fromBlogDetailContract,
    toUpdateBlogCommentRequestContract(request),
  )
}

export async function deleteBlogComment(blogId: BlogId, commentId: BlogCommentId): Promise<BlogDetail> {
  return postJson(`/api/blogs/${blogIdValue(blogId)}/comments/${blogCommentIdValue(commentId)}/delete`, fromBlogDetailContract, {})
}
