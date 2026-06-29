package domains.blog.objects.request

import domains.blog.objects.*
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

/** 博客投票请求体，携带目标投票方向。 */
final case class VoteBlogRequest(
  vote: BlogVote
)

/** 提供博客投票请求体 JSON codec。 */
object VoteBlogRequest:
  given Encoder[VoteBlogRequest] = deriveEncoder[VoteBlogRequest]
  given Decoder[VoteBlogRequest] = deriveDecoder[VoteBlogRequest]
