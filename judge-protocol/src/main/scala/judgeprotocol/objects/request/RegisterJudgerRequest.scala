package judgeprotocol.objects.request

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import judgeprotocol.objects.{JudgerId, SubmissionLanguage}

/** judger 启动时向 backend 注册能力和主机身份的请求。 */
final case class RegisterJudgerRequest(
  preferredPrefix: JudgerId,
  host: String,
  processId: Option[String],
  supportedLanguages: List[SubmissionLanguage]
)

/** 负责 judger 注册请求的协议编解码。 */
object RegisterJudgerRequest:
  given Encoder[RegisterJudgerRequest] = deriveEncoder[RegisterJudgerRequest]
  given Decoder[RegisterJudgerRequest] = deriveDecoder[RegisterJudgerRequest]
