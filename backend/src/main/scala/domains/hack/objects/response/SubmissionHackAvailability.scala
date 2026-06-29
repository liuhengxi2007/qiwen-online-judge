package domains.hack.objects.response

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

/** 目标提交的 hack 可用性响应；按子任务列出是否允许创建 hack。 */
final case class SubmissionHackAvailability(
  subtasks: List[HackSubtaskAvailability]
)

/** SubmissionHackAvailability 的 JSON 编解码器。 */
object SubmissionHackAvailability:
  given Encoder[SubmissionHackAvailability] = deriveEncoder[SubmissionHackAvailability]
  given Decoder[SubmissionHackAvailability] = deriveDecoder[SubmissionHackAvailability]
