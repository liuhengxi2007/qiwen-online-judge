package domains.user.objects



import domains.problem.objects.{ProblemSlug, ProblemTitle}
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
  private given Encoder[Instant] = Encoder.encodeString.contramap(_.toString)
  private given Decoder[Instant] = Decoder.decodeString.emap { value =>
    Try(Instant.parse(value)).toEither.left.map(_.getMessage)
  }

  given Encoder[UserAcceptedProblem] = deriveEncoder[UserAcceptedProblem]
  given Decoder[UserAcceptedProblem] = deriveDecoder[UserAcceptedProblem]
