package judgeprotocol.objects.response

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

final case class JudgeTaskFileRef(
  path: String,
  sizeBytes: Long,
  sha256: String
)

object JudgeTaskFileRef:
  given Encoder[JudgeTaskFileRef] = deriveEncoder[JudgeTaskFileRef]
  given Decoder[JudgeTaskFileRef] = deriveDecoder[JudgeTaskFileRef]
