package judgeprotocol.objects.response

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import judgeprotocol.objects.JudgerId

final case class RegisterJudgerResponse(
  judgerId: JudgerId,
  heartbeatIntervalMs: Long,
  heartbeatTimeoutMs: Long
)

object RegisterJudgerResponse:
  given Encoder[RegisterJudgerResponse] = deriveEncoder[RegisterJudgerResponse]
  given Decoder[RegisterJudgerResponse] = deriveDecoder[RegisterJudgerResponse]
