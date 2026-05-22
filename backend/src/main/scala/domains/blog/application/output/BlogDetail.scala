package domains.blog.application.output

import domains.blog.model.*

import domains.user.model.UserIdentity

import java.time.Instant

final case class BlogDetail(
  id: BlogId,
  title: BlogTitle,
  content: BlogContent,
  author: UserIdentity,
  visibility: BlogVisibility,
  relatedProblems: List[BlogProblemReference],
  score: Int,
  viewerVote: Option[BlogVote],
  comments: List[BlogCommentSummary],
  createdAt: Instant,
  updatedAt: Instant
)
