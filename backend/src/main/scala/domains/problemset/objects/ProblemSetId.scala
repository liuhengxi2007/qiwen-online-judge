package domains.problemset.objects

import io.circe.{Decoder, Encoder}


import java.util.UUID
import scala.util.Try

final case class ProblemSetId(value: UUID)

object ProblemSetId:
  given Encoder[ProblemSetId] = Encoder.encodeString.contramap(_.value.toString)
  given Decoder[ProblemSetId] = Decoder.decodeString.emap { value =>
    Try(UUID.fromString(value)).toEither.left.map(_.getMessage).map(ProblemSetId(_))
  }
