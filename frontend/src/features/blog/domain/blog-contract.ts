import type {
  BlogListResponse as BlogListResponseContract,
  BlogCommentSummary as BlogCommentSummaryContract,
  BlogDetail as BlogDetailContract,
  BlogSummary as BlogSummaryContract,
  CreateBlogCommentRequest as CreateBlogCommentRequestContract,
  CreateBlogRequest as CreateBlogRequestContract,
  UpdateBlogCommentRequest as UpdateBlogCommentRequestContract,
  UpdateBlogRequest as UpdateBlogRequestContract,
  VoteBlogCommentRequest as VoteBlogCommentRequestContract,
  VoteBlogRequest as VoteBlogRequestContract,
} from '@contracts/blog'
import { fromUserIdentityContract } from '@/features/auth/domain/auth'
import type { BlogCommentSummary } from '@/features/blog/model/BlogCommentSummary'
import type { BlogDetail } from '@/features/blog/model/BlogDetail'
import type { BlogListResponse } from '@/features/blog/model/BlogListResponse'
import type { BlogProblemReference } from '@/features/blog/model/BlogProblemReference'
import type { BlogSummary } from '@/features/blog/model/BlogSummary'
import type { CreateBlogCommentRequest } from '@/features/blog/model/CreateBlogCommentRequest'
import type { CreateBlogRequest } from '@/features/blog/model/CreateBlogRequest'
import type { UpdateBlogCommentRequest } from '@/features/blog/model/UpdateBlogCommentRequest'
import type { UpdateBlogRequest } from '@/features/blog/model/UpdateBlogRequest'
import type { VoteBlogCommentRequest } from '@/features/blog/model/VoteBlogCommentRequest'
import type { VoteBlogRequest } from '@/features/blog/model/VoteBlogRequest'
import {
  parseProblemSlug,
  parseProblemTitle,
  requireParsed as requireParsedProblem,
} from '@/features/problem/domain/problem-parsers'
import {
  blogCommentContentValue,
  blogContentValue,
  blogTitleValue,
  parseBlogCommentContent,
  parseBlogCommentId,
  parseBlogContent,
  parseBlogId,
  parseBlogTitle,
  requireParsed,
} from '@/features/blog/domain/blog-parsers'

export function fromBlogSummaryContract(blog: BlogSummaryContract): BlogSummary {
  return {
    id: requireParsed(parseBlogId(blog.id), 'blog id'),
    title: requireParsed(parseBlogTitle(blog.title), 'blog title'),
    content: requireParsed(parseBlogContent(blog.content), 'blog content'),
    author: fromUserIdentityContract(blog.author),
    visibility: blog.visibility,
    relatedProblems: parseRelatedProblems(blog.relatedProblems),
    score: blog.score,
    viewerVote: blog.viewerVote,
    createdAt: blog.createdAt,
    updatedAt: blog.updatedAt,
  }
}

export function fromBlogCommentSummaryContract(comment: BlogCommentSummaryContract): BlogCommentSummary {
  return {
    id: requireParsed(parseBlogCommentId(comment.id), 'blog comment id'),
    parentId: comment.parentId === null ? null : requireParsed(parseBlogCommentId(comment.parentId), 'blog comment parent id'),
    content: requireParsed(parseBlogCommentContent(comment.content), 'blog comment content'),
    author: fromUserIdentityContract(comment.author),
    score: comment.score,
    viewerVote: comment.viewerVote,
    createdAt: comment.createdAt,
    updatedAt: comment.updatedAt,
  }
}

export function fromBlogDetailContract(blog: BlogDetailContract): BlogDetail {
  return {
    id: requireParsed(parseBlogId(blog.id), 'blog id'),
    title: requireParsed(parseBlogTitle(blog.title), 'blog title'),
    content: requireParsed(parseBlogContent(blog.content), 'blog content'),
    author: fromUserIdentityContract(blog.author),
    visibility: blog.visibility,
    relatedProblems: parseRelatedProblems(blog.relatedProblems),
    score: blog.score,
    viewerVote: blog.viewerVote,
    comments: blog.comments.map(fromBlogCommentSummaryContract),
    createdAt: blog.createdAt,
    updatedAt: blog.updatedAt,
  }
}

export function fromBlogListResponseContract(response: BlogListResponseContract): BlogListResponse {
  return response.map(fromBlogSummaryContract)
}

export function toCreateBlogRequestContract(request: CreateBlogRequest): CreateBlogRequestContract {
  return {
    title: blogTitleValue(request.title),
    content: blogContentValue(request.content),
    visibility: request.visibility,
  }
}

export function toUpdateBlogRequestContract(request: UpdateBlogRequest): UpdateBlogRequestContract {
  return {
    title: blogTitleValue(request.title),
    content: blogContentValue(request.content),
    visibility: request.visibility,
  }
}

function parseRelatedProblems(relatedProblems: BlogSummaryContract['relatedProblems']): BlogProblemReference[] {
  return relatedProblems.map((problem, index) => ({
    slug: requireParsedProblem(parseProblemSlug(problem.slug), `blog related problem slug ${index}`),
    title: requireParsedProblem(parseProblemTitle(problem.title), `blog related problem title ${index}`),
  }))
}

export function toVoteBlogRequestContract(request: VoteBlogRequest): VoteBlogRequestContract {
  return {
    vote: request.vote,
  }
}

export function toCreateBlogCommentRequestContract(request: CreateBlogCommentRequest): CreateBlogCommentRequestContract {
  return {
    content: blogCommentContentValue(request.content),
  }
}

export function toUpdateBlogCommentRequestContract(request: UpdateBlogCommentRequest): UpdateBlogCommentRequestContract {
  return {
    content: blogCommentContentValue(request.content),
  }
}

export function toVoteBlogCommentRequestContract(request: VoteBlogCommentRequest): VoteBlogCommentRequestContract {
  return {
    vote: request.vote,
  }
}
