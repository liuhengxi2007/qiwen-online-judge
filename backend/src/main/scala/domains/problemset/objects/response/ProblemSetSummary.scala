package domains.problemset.objects.response

import domains.problemset.objects.*

import domains.user.objects.UserIdentity
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import shared.objects.access.ResourceVisibilityPolicy

import java.time.Instant
import scala.util.Try

/** 题单列表摘要响应，不携带题目列表。 */
final case class ProblemSetSummary(
  id: ProblemSetId,
  slug: ProblemSetSlug,
  title: ProblemSetTitle,
  description: ProblemSetDescription,
  accessPolicy: ResourceVisibilityPolicy,
  author: Option[UserIdentity],
  createdAt: Instant,
  updatedAt: Instant
)

/** 提供题单摘要 JSON codec，并显式处理 Instant 字符串格式。 */
object ProblemSetSummary:
  private given Encoder[Instant] = Encoder.encodeString.contramap(_.toString)
  private given Decoder[Instant] = Decoder.decodeString.emap { value =>
    Try(Instant.parse(value)).toEither.left.map(_.getMessage)
  }

  given Encoder[ProblemSetSummary] = deriveEncoder[ProblemSetSummary]
  given Decoder[ProblemSetSummary] = deriveDecoder[ProblemSetSummary]
