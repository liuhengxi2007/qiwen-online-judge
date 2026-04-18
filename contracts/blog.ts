import type { UserIdentity } from './auth'

export type CreateBlogRequest = {
  title: string
  content: string
  visibility: BlogVisibility
  blogType: BlogType
  problemSlug: string | null
}

export type UpdateBlogRequest = {
  title: string
  content: string
  visibility: BlogVisibility
  blogType: BlogType
  problemSlug: string | null
}

export type BlogType = 'general' | 'problem'
export type BlogVisibility = 'public' | 'private'
export type BlogVote = 'up' | 'down'

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
  blogType: BlogType
  problemSlug: string | null
  problemTitle: string | null
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
  blogType: BlogType
  problemSlug: string | null
  problemTitle: string | null
  score: number
  viewerVote: BlogVote | null
  comments: BlogCommentSummary[]
  createdAt: string
  updatedAt: string
}

export type BlogListResponse = BlogSummary[]
