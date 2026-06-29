package judgeprotocol.objects.request

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import judgeprotocol.objects.JudgerId

/** worker 领取判题或 hack 任务时携带当前 judger 租约标识。 */
final case class ClaimJudgeTaskRequest(judgerId: JudgerId)

/** 负责任务领取请求的协议编解码。 */
object ClaimJudgeTaskRequest:
  given Encoder[ClaimJudgeTaskRequest] = deriveEncoder[ClaimJudgeTaskRequest]
  given Decoder[ClaimJudgeTaskRequest] = deriveDecoder[ClaimJudgeTaskRequest]
