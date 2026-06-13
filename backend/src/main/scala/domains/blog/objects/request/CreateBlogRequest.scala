package domains.blog.objects.request

import domains.blog.objects.*
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

/** 创建博客请求体，包含标题、正文和初始可见性。 */
final case class CreateBlogRequest(
  title: BlogTitle,
  content: BlogContent,
  visibility: BlogVisibility
)

/** 提供创建博客请求体 JSON codec。 */
object CreateBlogRequest:
  given Encoder[CreateBlogRequest] = deriveEncoder[CreateBlogRequest]
  given Decoder[CreateBlogRequest] = deriveDecoder[CreateBlogRequest]
