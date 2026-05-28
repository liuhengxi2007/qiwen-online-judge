package domains.problem.objects

import io.circe.{Decoder, Encoder}


import java.util.UUID
import scala.util.Try

final case class ProblemId(value: UUID)

object ProblemId:
  given Encoder[ProblemId] = Encoder.encodeString.contramap(_.value.toString)
  given Decoder[ProblemId] = Decoder.decodeString.emap { value =>
    Try(UUID.fromString(value)).toEither.left.map(_.getMessage).map(ProblemId(_))
  }
