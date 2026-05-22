package domains.user.model



import domains.problem.model.{ProblemSlug, ProblemTitle}
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

import java.time.Instant
import scala.util.Try

final case class UserAcceptedProblem(
  slug: ProblemSlug,
  title: ProblemTitle,
  acceptedAt: Instant
)

object UserAcceptedProblem:
  private given instantEncoder: Encoder[Instant] = Encoder.encodeString.contramap(_.toString)
  private given instantDecoder: Decoder[Instant] = Decoder.decodeString.emap { value =>
    Try(Instant.parse(value)).toEither.left.map(_.getMessage)
  }

  given Encoder[UserAcceptedProblem] = deriveEncoder[UserAcceptedProblem]
  given Decoder[UserAcceptedProblem] = deriveDecoder[UserAcceptedProblem]
