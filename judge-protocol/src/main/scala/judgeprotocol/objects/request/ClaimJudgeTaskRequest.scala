package judgeprotocol.objects.request

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import judgeprotocol.objects.JudgerId

final case class ClaimJudgeTaskRequest(judgerId: JudgerId)

object ClaimJudgeTaskRequest:
  given Encoder[ClaimJudgeTaskRequest] = deriveEncoder[ClaimJudgeTaskRequest]
  given Decoder[ClaimJudgeTaskRequest] = deriveDecoder[ClaimJudgeTaskRequest]
