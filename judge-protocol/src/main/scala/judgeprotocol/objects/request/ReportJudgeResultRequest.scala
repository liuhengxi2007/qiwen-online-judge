package judgeprotocol.objects.request

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import judgeprotocol.objects.SubmissionStatus
import judgeprotocol.objects.response.JudgeResult

/** judger 完成普通提交判题后回报给 backend 的状态和结果树。 */
final case class ReportJudgeResultRequest(
  status: SubmissionStatus,
  judgeResult: Option[JudgeResult]
)

/** 负责普通判题结果回报请求的协议编解码。 */
object ReportJudgeResultRequest:
  given Encoder[ReportJudgeResultRequest] = deriveEncoder[ReportJudgeResultRequest]
  given Decoder[ReportJudgeResultRequest] = deriveDecoder[ReportJudgeResultRequest]
