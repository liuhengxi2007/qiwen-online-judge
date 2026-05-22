package domains.blog.application.input

import domains.blog.model.*

final case class VoteBlogCommentRequest(
  vote: BlogVote
)
