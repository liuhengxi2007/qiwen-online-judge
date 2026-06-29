package domains.blog.objects.request

import domains.blog.objects.*
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import shared.objects.access.ResourceVisibilityPolicy

/** 更新博客请求体，包含新的标题、正文和可见性策略。 */
final case class UpdateBlogRequest(
  title: BlogTitle,
  content: BlogContent,
  visibilityPolicy: ResourceVisibilityPolicy
)

/** 提供更新博客请求体 JSON codec。 */
object UpdateBlogRequest:
  given Encoder[UpdateBlogRequest] = deriveEncoder[UpdateBlogRequest]
  given Decoder[UpdateBlogRequest] = deriveDecoder[UpdateBlogRequest]
