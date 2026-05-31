package domains.problemset.objects.response

import domains.problemset.objects.*

import domains.user.objects.UserIdentity
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import shared.objects.access.ResourceAccessPolicy

import java.time.Instant
import scala.util.Try

final case class ProblemSetSummary(
  id: ProblemSetId,
  slug: ProblemSetSlug,
  title: ProblemSetTitle,
  description: ProblemSetDescription,
  accessPolicy: ResourceAccessPolicy,
  author: Option[UserIdentity],
  createdAt: Instant,
  updatedAt: Instant
)

object ProblemSetSummary:
  private given Encoder[Instant] = Encoder.encodeString.contramap(_.toString)
  private given Decoder[Instant] = Decoder.decodeString.emap { value =>
    Try(Instant.parse(value)).toEither.left.map(_.getMessage)
  }

  given Encoder[ProblemSetSummary] = deriveEncoder[ProblemSetSummary]
  given Decoder[ProblemSetSummary] = deriveDecoder[ProblemSetSummary]
