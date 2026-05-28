package domains.blog.objects.request

import domains.blog.objects.*
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

final case class VoteBlogCommentRequest(
  vote: BlogVote
)

object VoteBlogCommentRequest:
  given Encoder[VoteBlogCommentRequest] = deriveEncoder[VoteBlogCommentRequest]
  given Decoder[VoteBlogCommentRequest] = deriveDecoder[VoteBlogCommentRequest]
