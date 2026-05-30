package judgeprotocol.objects.request

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

final case class JudgerHeartbeatRequest()

object JudgerHeartbeatRequest:
  given Encoder[JudgerHeartbeatRequest] = deriveEncoder[JudgerHeartbeatRequest]
  given Decoder[JudgerHeartbeatRequest] = deriveDecoder[JudgerHeartbeatRequest]
