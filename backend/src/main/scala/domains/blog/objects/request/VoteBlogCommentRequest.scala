package domains.blog.objects.request

import domains.blog.objects.*

final case class VoteBlogCommentRequest(
  vote: BlogVote
)
