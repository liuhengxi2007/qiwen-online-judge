package shared.access



import io.circe.{Decoder, Encoder}

import java.util.UUID
import scala.util.Try

final case class ResourceId(value: UUID)

object ResourceId:

  given Encoder[ResourceId] = Encoder.encodeString.contramap(_.value.toString)
  given Decoder[ResourceId] = Decoder.decodeString.emap { value =>
    Try(UUID.fromString(value)).toEither.left.map(_.getMessage).map(ResourceId(_))
  }
