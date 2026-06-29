package domains.blog.objects.request

import domains.blog.objects.*
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

/** 创建博客评论或回复的请求体。 */
final case class CreateBlogCommentRequest(
  content: BlogCommentContent
)

/** 提供创建评论请求体 JSON codec。 */
object CreateBlogCommentRequest:
  given Encoder[CreateBlogCommentRequest] = deriveEncoder[CreateBlogCommentRequest]
  given Decoder[CreateBlogCommentRequest] = deriveDecoder[CreateBlogCommentRequest]
