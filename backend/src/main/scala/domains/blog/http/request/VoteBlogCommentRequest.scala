package domains.blog.http.request

import domains.blog.model.*

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

final case class VoteBlogCommentRequest(
  vote: BlogVote
)

object VoteBlogCommentRequest:
  given Encoder[VoteBlogCommentRequest] = deriveEncoder[VoteBlogCommentRequest]
  given Decoder[VoteBlogCommentRequest] = deriveDecoder[VoteBlogCommentRequest]
