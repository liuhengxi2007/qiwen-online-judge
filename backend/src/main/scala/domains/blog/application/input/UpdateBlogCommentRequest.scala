package domains.blog.application.input

import domains.blog.model.*

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

final case class UpdateBlogCommentRequest(
  content: BlogCommentContent
)

object UpdateBlogCommentRequest:
  given Encoder[UpdateBlogCommentRequest] = deriveEncoder[UpdateBlogCommentRequest]
  given Decoder[UpdateBlogCommentRequest] = deriveDecoder[UpdateBlogCommentRequest]
