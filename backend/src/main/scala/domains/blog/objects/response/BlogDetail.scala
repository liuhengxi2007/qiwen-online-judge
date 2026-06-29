package domains.blog.objects.response

import domains.blog.objects.*

import domains.user.objects.UserIdentity
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import shared.objects.access.ResourceVisibilityPolicy

import java.time.Instant
import scala.util.Try

/** 博客详情响应，包含正文、关联题目、投票信息和评论列表。 */
final case class BlogDetail(
  id: BlogId,
  title: BlogTitle,
  content: BlogContent,
  author: UserIdentity,
  visibilityPolicy: ResourceVisibilityPolicy,
  relatedProblems: List[BlogProblemReference],
  score: Int,
  viewerVote: Option[BlogVote],
  comments: List[BlogCommentSummary],
  createdAt: Instant,
  updatedAt: Instant
)

/** 提供博客详情 JSON codec，并显式处理 Instant 字符串格式。 */
object BlogDetail:
  private given Encoder[Instant] = Encoder.encodeString.contramap(_.toString)
  private given Decoder[Instant] = Decoder.decodeString.emap { value =>
    Try(Instant.parse(value)).toEither.left.map(_.getMessage)
  }

  given Encoder[BlogDetail] = deriveEncoder[BlogDetail]
  given Decoder[BlogDetail] = deriveDecoder[BlogDetail]
