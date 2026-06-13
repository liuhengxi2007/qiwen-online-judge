package domains.blog.objects.request

import domains.blog.objects.*
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

/** 更新博客评论的请求体。 */
final case class UpdateBlogCommentRequest(
  content: BlogCommentContent
)

/** 提供更新评论请求体 JSON codec。 */
object UpdateBlogCommentRequest:
  given Encoder[UpdateBlogCommentRequest] = deriveEncoder[UpdateBlogCommentRequest]
  given Decoder[UpdateBlogCommentRequest] = deriveDecoder[UpdateBlogCommentRequest]
