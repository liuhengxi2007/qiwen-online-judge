package judgeprotocol.objects.response

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import judgeprotocol.objects.JudgerId

/** backend 注册 judger 后返回的租约信息和心跳时间参数。 */
final case class RegisterJudgerResponse(
  judgerId: JudgerId,
  heartbeatIntervalMs: Long,
  heartbeatTimeoutMs: Long
)

/** 负责 judger 注册响应的协议编解码。 */
object RegisterJudgerResponse:
  given Encoder[RegisterJudgerResponse] = deriveEncoder[RegisterJudgerResponse]
  given Decoder[RegisterJudgerResponse] = deriveDecoder[RegisterJudgerResponse]
