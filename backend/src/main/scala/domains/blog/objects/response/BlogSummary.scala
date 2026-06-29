package domains.blog.objects.response

import domains.blog.objects.*

import domains.user.objects.UserIdentity
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import shared.objects.access.ResourceVisibilityPolicy

import java.time.Instant
import scala.util.Try

/** 博客列表摘要响应，包含正文预览来源、关联题目和当前用户投票。 */
final case class BlogSummary(
  id: BlogId,
  title: BlogTitle,
  content: BlogContent,
  author: UserIdentity,
  visibilityPolicy: ResourceVisibilityPolicy,
  relatedProblems: List[BlogProblemReference],
  score: Int,
  viewerVote: Option[BlogVote],
  createdAt: Instant,
  updatedAt: Instant
)

/** 提供博客摘要 JSON codec，并显式处理 Instant 字符串格式。 */
object BlogSummary:
  private given Encoder[Instant] = Encoder.encodeString.contramap(_.toString)
  private given Decoder[Instant] = Decoder.decodeString.emap { value =>
    Try(Instant.parse(value)).toEither.left.map(_.getMessage)
  }

  given Encoder[BlogSummary] = deriveEncoder[BlogSummary]
  given Decoder[BlogSummary] = deriveDecoder[BlogSummary]
