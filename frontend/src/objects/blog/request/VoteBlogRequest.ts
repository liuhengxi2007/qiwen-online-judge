import type { BlogVote } from '@/objects/blog/BlogVote'

/** 博客投票请求体；目标博客由 API path 指定。 */
export type VoteBlogRequest = {
  vote: BlogVote
}
