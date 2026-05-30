package judgeprotocol.objects.response

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

final case class JudgeTaskChecker(
  `type`: String,
  name: Option[String],
  source: Option[JudgeTaskFileRef]
)

object JudgeTaskChecker:
  given Encoder[JudgeTaskChecker] = deriveEncoder[JudgeTaskChecker]
  given Decoder[JudgeTaskChecker] = deriveDecoder[JudgeTaskChecker]
