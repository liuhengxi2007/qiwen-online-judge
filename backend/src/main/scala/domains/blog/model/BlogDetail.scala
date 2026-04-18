package domains.blog.model

import domains.auth.model.UserIdentity
import domains.problem.model.{ProblemSlug, ProblemTitle}
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

import java.time.Instant
import scala.util.Try

final case class BlogDetail(
  id: BlogId,
  title: BlogTitle,
  content: BlogContent,
  author: UserIdentity,
  visibility: BlogVisibility,
  blogType: BlogType,
  problemSlug: Option[ProblemSlug],
  problemTitle: Option[ProblemTitle],
  score: Int,
  viewerVote: Option[BlogVote],
  comments: List[BlogCommentSummary],
  createdAt: Instant,
  updatedAt: Instant
)

object BlogDetail:
  private given Encoder[Instant] = Encoder.encodeString.contramap(_.toString)
  private given Decoder[Instant] = Decoder.decodeString.emap { value =>
    Try(Instant.parse(value)).toEither.left.map(_.getMessage)
  }

  given Encoder[BlogDetail] = deriveEncoder[BlogDetail]
  given Decoder[BlogDetail] = deriveDecoder[BlogDetail]
