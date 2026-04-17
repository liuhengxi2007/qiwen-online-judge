package domains.problem.model

import domains.auth.model.UserIdentity
import domains.shared.access.ResourceAccessPolicy
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

import java.time.Instant
import scala.util.Try

final case class ProblemSummary(
  id: ProblemId,
  slug: ProblemSlug,
  title: ProblemTitle,
  data: ProblemData,
  timeLimitMs: ProblemTimeLimitMs,
  spaceLimitMb: ProblemSpaceLimitMb,
  accessPolicy: ResourceAccessPolicy,
  othersSubmissionAccess: OthersSubmissionAccess,
  creator: UserIdentity,
  createdAt: Instant,
  updatedAt: Instant
)

object ProblemSummary:
  private given instantEncoder: Encoder[Instant] = Encoder.encodeString.contramap(_.toString)
  private given instantDecoder: Decoder[Instant] = Decoder.decodeString.emap { value =>
    Try(Instant.parse(value)).toEither.left.map(_.getMessage)
  }

  given Encoder[ProblemSummary] = deriveEncoder[ProblemSummary]
  given Decoder[ProblemSummary] = deriveDecoder[ProblemSummary]
