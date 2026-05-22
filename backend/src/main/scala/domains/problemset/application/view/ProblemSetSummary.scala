package domains.problemset.application.view

import domains.problemset.model.*

import domains.user.model.UserIdentity
import domains.shared.access.ResourceAccessPolicy
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

import java.time.Instant
import scala.util.Try

final case class ProblemSetSummary(
  id: ProblemSetId,
  slug: ProblemSetSlug,
  title: ProblemSetTitle,
  description: ProblemSetDescription,
  accessPolicy: ResourceAccessPolicy,
  creator: UserIdentity,
  createdAt: Instant,
  updatedAt: Instant
)

object ProblemSetSummary:
  given Encoder[Instant] = Encoder.encodeString.contramap(_.toString)
  given Decoder[Instant] = Decoder.decodeString.emap { value =>
    Try(Instant.parse(value)).toEither.left.map(_.getMessage)
  }

  given Encoder[ProblemSetSummary] = deriveEncoder[ProblemSetSummary]
  given Decoder[ProblemSetSummary] = deriveDecoder[ProblemSetSummary]
