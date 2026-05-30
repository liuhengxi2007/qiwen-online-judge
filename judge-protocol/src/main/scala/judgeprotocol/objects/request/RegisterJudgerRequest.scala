package judgeprotocol.objects.request

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import judgeprotocol.objects.{JudgerId, SubmissionLanguage}

final case class RegisterJudgerRequest(
  preferredPrefix: JudgerId,
  host: String,
  processId: Option[String],
  supportedLanguages: List[SubmissionLanguage]
)

object RegisterJudgerRequest:
  given Encoder[RegisterJudgerRequest] = deriveEncoder[RegisterJudgerRequest]
  given Decoder[RegisterJudgerRequest] = deriveDecoder[RegisterJudgerRequest]
