package domains.hack.objects.request

import domains.submission.objects.SubmissionId
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

/** 创建 hack 的请求体；input 是测试输入，strategyProviderSource 仅交互式策略子任务需要。 */
final case class CreateHackRequest(
  targetSubmissionId: SubmissionId,
  subtaskIndex: Int,
  input: String,
  strategyProviderSource: Option[String]
)

/** CreateHackRequest 的 JSON 编解码器。 */
object CreateHackRequest:
  given Encoder[CreateHackRequest] = deriveEncoder[CreateHackRequest]
  given Decoder[CreateHackRequest] = deriveDecoder[CreateHackRequest]
