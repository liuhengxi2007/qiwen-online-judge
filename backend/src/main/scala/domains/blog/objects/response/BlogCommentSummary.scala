package domains.blog.objects.response

import domains.blog.objects.*

import domains.user.objects.UserIdentity

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
