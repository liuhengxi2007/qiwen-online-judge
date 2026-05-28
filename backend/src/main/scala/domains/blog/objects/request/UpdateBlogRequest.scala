package domains.blog.objects.request

import domains.blog.objects.*
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
