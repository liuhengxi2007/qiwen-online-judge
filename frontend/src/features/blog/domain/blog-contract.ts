import { fromUserIdentityContract } from '@/features/user/domain/user'
import type { BlogCommentSummary } from '@/features/blog/http/response/BlogCommentSummary'
import type { BlogDetail } from '@/features/blog/http/response/BlogDetail'
import type { BlogListResponse } from '@/features/blog/http/response/BlogListResponse'
import type { BlogProblemReference } from '@/features/blog/model/BlogProblemReference'
import type { BlogSummary } from '@/features/blog/http/response/BlogSummary'
import type { CreateBlogCommentRequest } from '@/features/blog/http/request/CreateBlogCommentRequest'
import type { CreateBlogRequest } from '@/features/blog/http/request/CreateBlogRequest'
import type { UpdateBlogCommentRequest } from '@/features/blog/http/request/UpdateBlogCommentRequest'
import type { UpdateBlogRequest } from '@/features/blog/http/request/UpdateBlogRequest'
import type { VoteBlogCommentRequest } from '@/features/blog/http/request/VoteBlogCommentRequest'
import type { VoteBlogRequest } from '@/features/blog/http/request/VoteBlogRequest'
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

type PageResponseContract<TItem> = {
  items: TItem[]
  page: number
  pageSize: number
  totalItems: number
}

type UserIdentityContract = {
  username: string
  displayName: string
}

type BlogVisibilityContract = 'public' | 'private'
type BlogVoteContract = 'up' | 'down'

type BlogProblemReferenceContract = {
  slug: string
  title: string
}

type CreateBlogRequestContract = {
  title: string
  content: string
  visibility: BlogVisibilityContract
}

type UpdateBlogRequestContract = {
  title: string
  content: string
  visibility: BlogVisibilityContract
}

type VoteBlogRequestContract = {
  vote: BlogVoteContract
}

type CreateBlogCommentRequestContract = {
  content: string
}

type UpdateBlogCommentRequestContract = {
  content: string
}

type VoteBlogCommentRequestContract = {
  vote: BlogVoteContract
}

type BlogCommentSummaryContract = {
  id: number
  parentId: number | null
  content: string
  author: UserIdentityContract
  score: number
  viewerVote: BlogVoteContract | null
  createdAt: string
  updatedAt: string
}

type BlogSummaryContract = {
  id: number
  title: string
  content: string
  author: UserIdentityContract
  visibility: BlogVisibilityContract
  relatedProblems: BlogProblemReferenceContract[]
  score: number
  viewerVote: BlogVoteContract | null
  createdAt: string
  updatedAt: string
}

type BlogDetailContract = {
  id: number
  title: string
  content: string
  author: UserIdentityContract
  visibility: BlogVisibilityContract
  relatedProblems: BlogProblemReferenceContract[]
  score: number
  viewerVote: BlogVoteContract | null
  comments: BlogCommentSummaryContract[]
  createdAt: string
  updatedAt: string
}

type BlogListResponseContract = PageResponseContract<BlogSummaryContract>

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
  return {
    items: response.items.map(fromBlogSummaryContract),
    page: response.page,
    pageSize: response.pageSize,
    totalItems: response.totalItems,
  }
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
