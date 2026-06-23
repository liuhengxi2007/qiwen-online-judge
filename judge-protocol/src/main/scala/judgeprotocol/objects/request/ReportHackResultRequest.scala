package judgeprotocol.objects.request

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import judgeprotocol.objects.response.JudgeResult

/** judger 完成 hack 尝试后回报的状态、可选答案、结果快照和诊断消息。 */
final case class ReportHackResultRequest(
  status: String,
  answer: Option[String],
  newResult: Option[JudgeResult],
  validatorMessage: Option[String],
  standardMessage: Option[String],
  targetMessage: Option[String]
)

/** 负责 hack 结果回报请求的协议编解码；status 仍由 backend HackStatus 解析校验。 */
object ReportHackResultRequest:
  given Encoder[ReportHackResultRequest] = deriveEncoder[ReportHackResultRequest]
  given Decoder[ReportHackResultRequest] = deriveDecoder[ReportHackResultRequest]
