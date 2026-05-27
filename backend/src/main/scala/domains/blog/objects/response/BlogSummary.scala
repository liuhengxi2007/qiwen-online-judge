package domains.blog.objects.response

import domains.blog.objects.*

import domains.user.objects.UserIdentity

import java.time.Instant

final case class BlogSummary(
  id: BlogId,
  title: BlogTitle,
  content: BlogContent,
  author: UserIdentity,
  visibility: BlogVisibility,
  relatedProblems: List[BlogProblemReference],
  score: Int,
  viewerVote: Option[BlogVote],
  createdAt: Instant,
  updatedAt: Instant
)
