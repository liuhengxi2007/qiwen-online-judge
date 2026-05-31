package judgeprotocol.objects.request

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import judgeprotocol.objects.SubmissionStatus
import judgeprotocol.objects.response.JudgeResult

final case class ReportJudgeResultRequest(
  status: SubmissionStatus,
  judgeResult: Option[JudgeResult]
)

object ReportJudgeResultRequest:
  given Encoder[ReportJudgeResultRequest] = deriveEncoder[ReportJudgeResultRequest]
  given Decoder[ReportJudgeResultRequest] = deriveDecoder[ReportJudgeResultRequest]
