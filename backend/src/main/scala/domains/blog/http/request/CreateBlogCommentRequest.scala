package domains.blog.http.request

import domains.blog.model.*

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

final case class CreateBlogCommentRequest(
  content: BlogCommentContent
)

object CreateBlogCommentRequest:
  given Encoder[CreateBlogCommentRequest] = deriveEncoder[CreateBlogCommentRequest]
  given Decoder[CreateBlogCommentRequest] = deriveDecoder[CreateBlogCommentRequest]
