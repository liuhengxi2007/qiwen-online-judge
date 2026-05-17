import type { UserIdentity } from './auth'
import type { PageResponse } from './shared'

export type CreateBlogRequest = {
  title: string
  content: string
  visibility: BlogVisibility
}

export type UpdateBlogRequest = {
  title: string
  content: string
  visibility: BlogVisibility
}

export type BlogVisibility = 'public' | 'private'
export type BlogVote = 'up' | 'down'

export type BlogProblemReference = {
  slug: string
  title: string
}

export type VoteBlogRequest = {
  vote: BlogVote
}

export type CreateBlogCommentRequest = {
  content: string
}

export type UpdateBlogCommentRequest = {
  content: string
}

export type VoteBlogCommentRequest = {
  vote: BlogVote
}

export type BlogCommentSummary = {
  id: number
  parentId: number | null
  content: string
  author: UserIdentity
  score: number
  viewerVote: BlogVote | null
  createdAt: string
  updatedAt: string
}

export type BlogSummary = {
  id: number
  title: string
  content: string
  author: UserIdentity
  visibility: BlogVisibility
  relatedProblems: BlogProblemReference[]
  score: number
  viewerVote: BlogVote | null
  createdAt: string
  updatedAt: string
}

export type BlogDetail = {
  id: number
  title: string
  content: string
  author: UserIdentity
  visibility: BlogVisibility
  relatedProblems: BlogProblemReference[]
  score: number
  viewerVote: BlogVote | null
  comments: BlogCommentSummary[]
  createdAt: string
  updatedAt: string
}

export type BlogListResponse = PageResponse<BlogSummary>
