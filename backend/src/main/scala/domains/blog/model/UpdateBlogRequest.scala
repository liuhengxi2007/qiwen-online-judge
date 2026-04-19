package domains.blog.model

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

final case class UpdateBlogRequest(
  title: BlogTitle,
  content: BlogContent,
  visibility: BlogVisibility
)

object UpdateBlogRequest:
  given Encoder[UpdateBlogRequest] = deriveEncoder[UpdateBlogRequest]
  given Decoder[UpdateBlogRequest] = deriveDecoder[UpdateBlogRequest]
