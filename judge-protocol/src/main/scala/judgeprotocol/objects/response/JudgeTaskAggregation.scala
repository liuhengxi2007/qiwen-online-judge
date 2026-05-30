package judgeprotocol.objects.response

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

final case class JudgeTaskAggregation(
  score: String,
  time: String,
  memory: String
)

object JudgeTaskAggregation:
  given Encoder[JudgeTaskAggregation] = deriveEncoder[JudgeTaskAggregation]
  given Decoder[JudgeTaskAggregation] = deriveDecoder[JudgeTaskAggregation]
