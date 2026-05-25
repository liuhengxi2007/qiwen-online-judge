import type { BlogCommentSummary } from '@/features/blog/model/response/BlogCommentSummary'
import type { BlogDetail } from '@/features/blog/model/response/BlogDetail'
import type { BlogListResponse } from '@/features/blog/model/response/BlogListResponse'
import type { BlogSummary } from '@/features/blog/model/response/BlogSummary'
import type { CreateBlogCommentRequest } from '@/features/blog/model/request/CreateBlogCommentRequest'
import type { CreateBlogRequest } from '@/features/blog/model/request/CreateBlogRequest'
import type { UpdateBlogCommentRequest } from '@/features/blog/model/request/UpdateBlogCommentRequest'
import type { UpdateBlogRequest } from '@/features/blog/model/request/UpdateBlogRequest'
import type { VoteBlogCommentRequest } from '@/features/blog/model/request/VoteBlogCommentRequest'
import type { VoteBlogRequest } from '@/features/blog/model/request/VoteBlogRequest'
import {
  fromBlogCommentContentContract,
  fromBlogCommentIdContract,
  fromBlogContentContract,
  fromBlogIdContract,
  fromBlogProblemReferenceContract,
  fromBlogTitleContract,
  fromBlogVisibilityContract,
  fromBlogVoteContract,
  toBlogCommentContentContract,
  toBlogContentContract,
  toBlogTitleContract,
  toBlogVisibilityContract,
  toBlogVoteContract,
  type BlogProblemReferenceContract,
  type BlogVisibilityContract,
  type BlogVoteContract,
} from '@/features/blog/http/codec/BlogModelHttpCodecs'
import {
  fromUserIdentityContract,
  type UserIdentityContract,
} from '@/features/user/http/codec/UserModelHttpCodecs'

type PageResponseContract<TItem> = {
  items: TItem[]
  page: number
  pageSize: number
  totalItems: number
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
