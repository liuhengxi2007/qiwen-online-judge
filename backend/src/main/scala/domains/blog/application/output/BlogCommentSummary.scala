package domains.blog.application.output

import domains.blog.model.*

import domains.user.model.UserIdentity

import java.time.Instant

final case class BlogCommentSummary(
  id: BlogCommentId,
  parentId: Option[BlogCommentId],
  content: BlogCommentContent,
  author: UserIdentity,
  score: Int,
  viewerVote: Option[BlogVote],
  createdAt: Instant,
  updatedAt: Instant
)
