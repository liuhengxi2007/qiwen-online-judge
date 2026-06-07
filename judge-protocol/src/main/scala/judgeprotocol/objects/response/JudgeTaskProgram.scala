package judgeprotocol.objects.response

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import judgeprotocol.objects.{SubmissionLanguage, SubmissionSourceCode}

final case class JudgeTaskProgram(
  language: SubmissionLanguage,
  sourceCode: SubmissionSourceCode,
  stub: Option[JudgeTaskFileRef] = None
)

object JudgeTaskProgram:
  given Encoder[JudgeTaskProgram] = deriveEncoder[JudgeTaskProgram]
  given Decoder[JudgeTaskProgram] = deriveDecoder[JudgeTaskProgram]
