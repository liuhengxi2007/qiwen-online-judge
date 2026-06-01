import type { UserIdentity } from '@/objects/user/UserIdentity'
import { fromUserIdentityContract } from '@/objects/user/UserIdentity'
import type { BlogCommentContent } from '@/objects/blog/BlogCommentContent'
import { fromBlogCommentContentContract } from '@/objects/blog/BlogCommentContent'
import type { BlogCommentId } from '@/objects/blog/BlogCommentId'
import { fromBlogCommentIdContract } from '@/objects/blog/BlogCommentId'
import type { BlogVote } from '@/objects/blog/BlogVote'
import { fromBlogVoteContract } from '@/objects/blog/BlogVote'
import { readNullable, readRecord, readSafeInteger, readString } from '@/objects/shared/PageResponse'

export type BlogCommentSummary = {
  id: BlogCommentId
  parentId: BlogCommentId | null
  content: BlogCommentContent
  author: UserIdentity
  score: number
  viewerVote: BlogVote | null
  createdAt: string
  updatedAt: string
}

export function fromBlogCommentSummaryContract(value: unknown, label: string): BlogCommentSummary {
  const comment = readRecord(value, label)
  return {
    id: fromBlogCommentIdContract(readSafeInteger(comment.id, `${label} id`), `${label} id`),
    parentId: readNullable(comment.parentId, `${label} parent id`, (parentId, parentLabel) =>
      fromBlogCommentIdContract(readSafeInteger(parentId, parentLabel), parentLabel),
    ),
    content: fromBlogCommentContentContract(readString(comment.content, `${label} content`), `${label} content`),
    author: fromUserIdentityContract(comment.author, `${label} author`),
    score: readSafeInteger(comment.score, `${label} score`),
    viewerVote: readNullable(comment.viewerVote, `${label} viewer vote`, fromBlogVoteContract),
    createdAt: readString(comment.createdAt, `${label} created at`),
    updatedAt: readString(comment.updatedAt, `${label} updated at`),
  }
}
