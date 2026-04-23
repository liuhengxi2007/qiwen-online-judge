package domains.shared.access

import io.circe.{Decoder, Encoder}

import java.util.UUID
import scala.util.Try

final case class ResourceId(value: UUID)

object ResourceId:
  def random(): ResourceId = ResourceId(UUID.randomUUID())

  given Encoder[ResourceId] = Encoder.encodeString.contramap(_.value.toString)
  given Decoder[ResourceId] = Decoder.decodeString.emap { value =>
    Try(UUID.fromString(value)).toEither.left.map(_.getMessage).map(ResourceId(_))
  }
