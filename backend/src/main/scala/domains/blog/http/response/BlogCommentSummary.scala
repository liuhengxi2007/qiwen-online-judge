package domains.blog.http.response

import domains.blog.model.*

import domains.user.model.UserIdentity
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

import java.time.Instant
import scala.util.Try

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

object BlogCommentSummary:
  private given Encoder[Instant] = Encoder.encodeString.contramap(_.toString)
  private given Decoder[Instant] = Decoder.decodeString.emap { value =>
    Try(Instant.parse(value)).toEither.left.map(_.getMessage)
  }

  given Encoder[BlogCommentSummary] = deriveEncoder[BlogCommentSummary]
  given Decoder[BlogCommentSummary] = deriveDecoder[BlogCommentSummary]
