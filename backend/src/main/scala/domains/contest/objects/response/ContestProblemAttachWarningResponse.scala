package domains.contest.objects.response

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

/** 添加赛题前的可见性提醒响应，shouldWarn 表示题目已有赛管之外受众。 */
final case class ContestProblemAttachWarningResponse(
  shouldWarn: Boolean
)

/** 提供添加赛题提醒响应 JSON codec。 */
object ContestProblemAttachWarningResponse:
  given Encoder[ContestProblemAttachWarningResponse] = deriveEncoder[ContestProblemAttachWarningResponse]
  given Decoder[ContestProblemAttachWarningResponse] = deriveDecoder[ContestProblemAttachWarningResponse]
