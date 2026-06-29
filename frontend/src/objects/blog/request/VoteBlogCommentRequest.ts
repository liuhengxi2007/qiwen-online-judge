import type { BlogVote } from '@/objects/blog/BlogVote'

/** 博客评论投票请求体；目标评论由 API path 指定。 */
export type VoteBlogCommentRequest = {
  vote: BlogVote
}
