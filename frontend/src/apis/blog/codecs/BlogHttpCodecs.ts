import type { BlogCommentSummary } from '@/objects/blog/response/BlogCommentSummary'
import type { BlogDetail } from '@/objects/blog/response/BlogDetail'
import type { BlogListResponse } from '@/objects/blog/response/BlogListResponse'
import type { BlogSummary } from '@/objects/blog/response/BlogSummary'
import type { CreateBlogCommentRequest } from '@/objects/blog/request/CreateBlogCommentRequest'
import type { CreateBlogRequest } from '@/objects/blog/request/CreateBlogRequest'
import type { UpdateBlogCommentRequest } from '@/objects/blog/request/UpdateBlogCommentRequest'
import type { UpdateBlogRequest } from '@/objects/blog/request/UpdateBlogRequest'
import type { VoteBlogCommentRequest } from '@/objects/blog/request/VoteBlogCommentRequest'
import type { VoteBlogRequest } from '@/objects/blog/request/VoteBlogRequest'
import { fromBlogCommentContentContract, toBlogCommentContentContract } from '@/objects/blog/BlogCommentContent'
import { fromBlogCommentIdContract } from '@/objects/blog/BlogCommentId'
import { fromBlogContentContract, toBlogContentContract } from '@/objects/blog/BlogContent'
import { fromBlogIdContract } from '@/objects/blog/BlogId'
import { fromBlogProblemReferenceContract } from '@/objects/blog/BlogProblemReference'
import { fromBlogTitleContract, toBlogTitleContract } from '@/objects/blog/BlogTitle'
import {
  fromBlogVisibilityContract,
  toBlogVisibilityContract,
  type BlogVisibility,
} from '@/objects/blog/BlogVisibility'
import { fromBlogVoteContract, toBlogVoteContract, type BlogVote } from '@/objects/blog/BlogVote'
import { fromUserIdentityContract } from '@/objects/user/UserIdentity'

type UserIdentityContract = {
  username: string
  displayName: string
}

type BlogProblemReferenceContract = {
  slug: string
  title: string
}

type PageResponseContract<TItem> = {
  items: TItem[]
  page: number
  pageSize: number
  totalItems: number
}

type CreateBlogRequestContract = {
  title: string
  content: string
  visibility: BlogVisibility
}

type UpdateBlogRequestContract = {
  title: string
  content: string
  visibility: BlogVisibility
}

type VoteBlogRequestContract = {
  vote: BlogVote
}

type CreateBlogCommentRequestContract = {
  content: string
}

type UpdateBlogCommentRequestContract = {
  content: string
}

type VoteBlogCommentRequestContract = {
  vote: BlogVote
}

type BlogCommentSummaryContract = {
  id: number
  parentId: number | null
  content: string
  author: UserIdentityContract
  score: number
  viewerVote: BlogVote | null
  createdAt: string
  updatedAt: string
}

type BlogSummaryContract = {
  id: number
  title: string
  content: string
  author: UserIdentityContract
  visibility: BlogVisibility
  relatedProblems: BlogProblemReferenceContract[]
  score: number
  viewerVote: BlogVote | null
  createdAt: string
  updatedAt: string
}

type BlogDetailContract = {
  id: number
  title: string
  content: string
  author: UserIdentityContract
  visibility: BlogVisibility
  relatedProblems: BlogProblemReferenceContract[]
  score: number
  viewerVote: BlogVote | null
  comments: BlogCommentSummaryContract[]
  createdAt: string
  updatedAt: string
}

type BlogListResponseContract = PageResponseContract<BlogSummaryContract>

export function fromBlogSummaryContract(blog: BlogSummaryContract): BlogSummary {
  return {
    id: fromBlogIdContract(blog.id, 'blog id'),
    title: fromBlogTitleContract(blog.title, 'blog title'),
    content: fromBlogContentContract(blog.content, 'blog content'),
    author: fromUserIdentityContract(blog.author),
    visibility: fromBlogVisibilityContract(blog.visibility),
    relatedProblems: parseRelatedProblems(blog.relatedProblems),
    score: blog.score,
    viewerVote: blog.viewerVote === null ? null : fromBlogVoteContract(blog.viewerVote),
    createdAt: blog.createdAt,
    updatedAt: blog.updatedAt,
  }
}

export function fromBlogCommentSummaryContract(comment: BlogCommentSummaryContract): BlogCommentSummary {
  return {
    id: fromBlogCommentIdContract(comment.id, 'blog comment id'),
    parentId: comment.parentId === null ? null : fromBlogCommentIdContract(comment.parentId, 'blog comment parent id'),
    content: fromBlogCommentContentContract(comment.content, 'blog comment content'),
    author: fromUserIdentityContract(comment.author),
    score: comment.score,
    viewerVote: comment.viewerVote === null ? null : fromBlogVoteContract(comment.viewerVote),
    createdAt: comment.createdAt,
    updatedAt: comment.updatedAt,
  }
}

export function fromBlogDetailContract(blog: BlogDetailContract): BlogDetail {
  return {
    id: fromBlogIdContract(blog.id, 'blog id'),
    title: fromBlogTitleContract(blog.title, 'blog title'),
    content: fromBlogContentContract(blog.content, 'blog content'),
    author: fromUserIdentityContract(blog.author),
    visibility: fromBlogVisibilityContract(blog.visibility),
    relatedProblems: parseRelatedProblems(blog.relatedProblems),
    score: blog.score,
    viewerVote: blog.viewerVote === null ? null : fromBlogVoteContract(blog.viewerVote),
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
    title: toBlogTitleContract(request.title),
    content: toBlogContentContract(request.content),
    visibility: toBlogVisibilityContract(request.visibility),
  }
}

export function toUpdateBlogRequestContract(request: UpdateBlogRequest): UpdateBlogRequestContract {
  return {
    title: toBlogTitleContract(request.title),
    content: toBlogContentContract(request.content),
    visibility: toBlogVisibilityContract(request.visibility),
  }
}

function parseRelatedProblems(relatedProblems: BlogSummaryContract['relatedProblems']) {
  return relatedProblems.map(fromBlogProblemReferenceContract)
}

export function toVoteBlogRequestContract(request: VoteBlogRequest): VoteBlogRequestContract {
  return {
    vote: toBlogVoteContract(request.vote),
  }
}

export function toCreateBlogCommentRequestContract(request: CreateBlogCommentRequest): CreateBlogCommentRequestContract {
  return {
    content: toBlogCommentContentContract(request.content),
  }
}

export function toUpdateBlogCommentRequestContract(request: UpdateBlogCommentRequest): UpdateBlogCommentRequestContract {
  return {
    content: toBlogCommentContentContract(request.content),
  }
}

export function toVoteBlogCommentRequestContract(request: VoteBlogCommentRequest): VoteBlogCommentRequestContract {
  return {
    vote: toBlogVoteContract(request.vote),
  }
}
