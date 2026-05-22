package domains.problemset.application.output

import domains.problemset.model.*

import domains.user.model.UserIdentity
import shared.access.ResourceAccessPolicy
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

import java.time.Instant
import scala.util.Try

final case class ProblemSetDetail(
  id: ProblemSetId,
  slug: ProblemSetSlug,
  title: ProblemSetTitle,
  description: ProblemSetDescription,
  problems: List[ProblemSetProblemSummary],
  accessPolicy: ResourceAccessPolicy,
  creator: UserIdentity,
  createdAt: Instant,
  updatedAt: Instant
)

object ProblemSetDetail:
  given Encoder[Instant] = Encoder.encodeString.contramap(_.toString)
  given Decoder[Instant] = Decoder.decodeString.emap { value =>
    Try(Instant.parse(value)).toEither.left.map(_.getMessage)
  }

  given Encoder[ProblemSetDetail] = deriveEncoder[ProblemSetDetail]
  given Decoder[ProblemSetDetail] = deriveDecoder[ProblemSetDetail]
