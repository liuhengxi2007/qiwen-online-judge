package domains.blog.model

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

final case class VoteBlogRequest(
  vote: BlogVote
)

object VoteBlogRequest:
  given Encoder[VoteBlogRequest] = deriveEncoder[VoteBlogRequest]
  given Decoder[VoteBlogRequest] = deriveDecoder[VoteBlogRequest]
