package judgeprotocol.objects.request

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import judgeprotocol.objects.response.JudgeResult

final case class ReportHackResultRequest(
  status: String,
  answer: Option[String],
  oldScore: BigDecimal,
  newScore: Option[BigDecimal],
  newResult: Option[JudgeResult],
  validatorMessage: Option[String],
  standardMessage: Option[String],
  targetMessage: Option[String]
)

object ReportHackResultRequest:
  given Encoder[ReportHackResultRequest] = deriveEncoder[ReportHackResultRequest]
  given Decoder[ReportHackResultRequest] = deriveDecoder[ReportHackResultRequest]
