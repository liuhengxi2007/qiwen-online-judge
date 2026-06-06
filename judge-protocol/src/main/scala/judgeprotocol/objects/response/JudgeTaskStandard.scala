package judgeprotocol.objects.response

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import judgeprotocol.objects.SubmissionLanguage

final case class JudgeTaskStandard(
  language: SubmissionLanguage,
  source: JudgeTaskFileRef
)

object JudgeTaskStandard:
  given Encoder[JudgeTaskStandard] = deriveEncoder[JudgeTaskStandard]
  given Decoder[JudgeTaskStandard] = deriveDecoder[JudgeTaskStandard]
