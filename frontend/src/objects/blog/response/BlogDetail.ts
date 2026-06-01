import type { UserIdentity } from '@/objects/user/UserIdentity'
import { fromUserIdentityContract } from '@/objects/user/UserIdentity'
import type { BlogCommentSummary } from '@/objects/blog/response/BlogCommentSummary'
import { fromBlogCommentSummaryContract } from '@/objects/blog/response/BlogCommentSummary'
import type { BlogContent } from '@/objects/blog/BlogContent'
import { fromBlogContentContract } from '@/objects/blog/BlogContent'
import type { BlogId } from '@/objects/blog/BlogId'
import { fromBlogIdContract } from '@/objects/blog/BlogId'
import type { BlogProblemReference } from '@/objects/blog/BlogProblemReference'
import { fromBlogProblemReferenceContract } from '@/objects/blog/BlogProblemReference'
import type { BlogTitle } from '@/objects/blog/BlogTitle'
import { fromBlogTitleContract } from '@/objects/blog/BlogTitle'
import type { BlogVisibility } from '@/objects/blog/BlogVisibility'
import { fromBlogVisibilityContract } from '@/objects/blog/BlogVisibility'
import type { BlogVote } from '@/objects/blog/BlogVote'
import { fromBlogVoteContract } from '@/objects/blog/BlogVote'
import { readArray, readNullable, readRecord, readSafeInteger, readString } from '@/objects/shared/PageResponse'

export type BlogDetail = {
  id: BlogId
  title: BlogTitle
  content: BlogContent
  author: UserIdentity
  visibility: BlogVisibility
  relatedProblems: BlogProblemReference[]
  score: number
  viewerVote: BlogVote | null
  comments: BlogCommentSummary[]
  createdAt: string
  updatedAt: string
}

export function fromBlogDetailContract(value: unknown, label = 'blog detail'): BlogDetail {
  const blog = readRecord(value, label)
  return {
    id: fromBlogIdContract(readSafeInteger(blog.id, `${label} id`), `${label} id`),
    title: fromBlogTitleContract(readString(blog.title, `${label} title`), `${label} title`),
    content: fromBlogContentContract(readString(blog.content, `${label} content`), `${label} content`),
    author: fromUserIdentityContract(blog.author, `${label} author`),
    visibility: fromBlogVisibilityContract(blog.visibility),
    relatedProblems: readArray(blog.relatedProblems, `${label} related problems`, fromBlogProblemReferenceContract),
    score: readSafeInteger(blog.score, `${label} score`),
    viewerVote: readNullable(blog.viewerVote, `${label} viewer vote`, fromBlogVoteContract),
    comments: readArray(blog.comments, `${label} comments`, fromBlogCommentSummaryContract),
    createdAt: readString(blog.createdAt, `${label} created at`),
    updatedAt: readString(blog.updatedAt, `${label} updated at`),
  }
}
