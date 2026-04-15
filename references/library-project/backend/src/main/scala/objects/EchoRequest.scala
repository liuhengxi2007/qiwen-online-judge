package objects

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

final case class EchoRequest(message: String, uppercase: Boolean)

object EchoRequest:
  given Encoder[EchoRequest] = deriveEncoder[EchoRequest]
  given Decoder[EchoRequest] = deriveDecoder[EchoRequest]
