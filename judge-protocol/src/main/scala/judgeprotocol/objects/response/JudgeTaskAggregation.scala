package judgeprotocol.objects.response

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

/** 分数、时间和内存的聚合策略名称，由 backend 构造，judger 执行。 */
final case class JudgeTaskAggregation(
  score: String,
  time: String,
  memory: String
)

/** 负责聚合策略的协议编解码；当前保留字符串形状以兼容既有配置。 */
object JudgeTaskAggregation:
  given Encoder[JudgeTaskAggregation] = deriveEncoder[JudgeTaskAggregation]
  given Decoder[JudgeTaskAggregation] = deriveDecoder[JudgeTaskAggregation]
