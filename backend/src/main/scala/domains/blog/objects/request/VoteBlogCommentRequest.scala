package domains.blog.objects.request

import domains.blog.objects.*
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

/** 评论投票请求体，携带目标投票方向。 */
final case class VoteBlogCommentRequest(
  vote: BlogVote
)

/** 提供评论投票请求体 JSON codec。 */
object VoteBlogCommentRequest:
  given Encoder[VoteBlogCommentRequest] = deriveEncoder[VoteBlogCommentRequest]
  given Decoder[VoteBlogCommentRequest] = deriveDecoder[VoteBlogCommentRequest]
