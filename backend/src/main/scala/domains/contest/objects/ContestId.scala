package domains.contest.objects

import io.circe.{Decoder, Encoder}

import java.util.UUID
import scala.util.Try

final case class ContestId(value: UUID)

object ContestId:
  given Encoder[ContestId] = Encoder.encodeString.contramap(_.value.toString)
  given Decoder[ContestId] = Decoder.decodeString.emap { value =>
    Try(UUID.fromString(value)).toEither.left.map(_.getMessage).map(ContestId(_))
  }
