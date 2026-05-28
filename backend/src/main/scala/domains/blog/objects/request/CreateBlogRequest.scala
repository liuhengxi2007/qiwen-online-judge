package domains.blog.objects.request

import domains.blog.objects.*
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

final case class CreateBlogRequest(
  title: BlogTitle,
  content: BlogContent,
  visibility: BlogVisibility
)

object CreateBlogRequest:
  given Encoder[CreateBlogRequest] = deriveEncoder[CreateBlogRequest]
  given Decoder[CreateBlogRequest] = deriveDecoder[CreateBlogRequest]
