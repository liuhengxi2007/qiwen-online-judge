package domains.blog.objects.response

import domains.blog.objects.*

import domains.user.objects.UserIdentity
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

import java.time.Instant
import scala.util.Try

/** 博客评论摘要响应，包含父评论、作者、分数和当前用户投票。 */
final case class BlogCommentSummary(
  id: BlogCommentId,
  parentId: Option[BlogCommentId],
  content: BlogCommentContent,
  author: UserIdentity,
  score: Int,
  viewerVote: Option[BlogVote],
  createdAt: Instant,
  updatedAt: Instant
)

/** 提供博客评论摘要 JSON codec，并显式处理 Instant 字符串格式。 */
object BlogCommentSummary:
  private given Encoder[Instant] = Encoder.encodeString.contramap(_.toString)
  private given Decoder[Instant] = Decoder.decodeString.emap { value =>
    Try(Instant.parse(value)).toEither.left.map(_.getMessage)
  }

  given Encoder[BlogCommentSummary] = deriveEncoder[BlogCommentSummary]
  given Decoder[BlogCommentSummary] = deriveDecoder[BlogCommentSummary]
