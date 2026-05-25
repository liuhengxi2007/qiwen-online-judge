package domains.judger.http.codec

import domains.judger.model.response.RegisteredJudgerListItem
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

import java.time.Instant
import scala.util.Try

object JudgerRegistryHttpCodecs:
  private given Encoder[Instant] = Encoder.encodeString.contramap(_.toString)
  private given Decoder[Instant] = Decoder.decodeString.emap { value =>
    Try(Instant.parse(value)).toEither.left.map(_.getMessage)
  }

  given Encoder[RegisteredJudgerListItem] = deriveEncoder[RegisteredJudgerListItem]
  given Decoder[RegisteredJudgerListItem] = deriveDecoder[RegisteredJudgerListItem]
