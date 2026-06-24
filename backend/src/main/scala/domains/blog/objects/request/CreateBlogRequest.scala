package domains.blog.objects.request

import domains.blog.objects.*
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import shared.objects.access.ResourceVisibilityPolicy

/** 创建博客请求体，包含标题、正文和初始可见性策略。 */
final case class CreateBlogRequest(
  title: BlogTitle,
  content: BlogContent,
  visibilityPolicy: ResourceVisibilityPolicy
)

/** 提供创建博客请求体 JSON codec。 */
object CreateBlogRequest:
  given Encoder[CreateBlogRequest] = deriveEncoder[CreateBlogRequest]
  given Decoder[CreateBlogRequest] = deriveDecoder[CreateBlogRequest]
