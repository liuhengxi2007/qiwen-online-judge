package domains.problem.objects.request

import domains.problem.objects.*

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import shared.objects.access.ResourceAccessPolicy

/** 创建题目的请求体；字段形状与前端镜像类型保持一致。 */
final case class CreateProblemRequest(
  slug: ProblemSlug,
  title: ProblemTitle,
  statement: ProblemStatementText,
  accessPolicy: ResourceAccessPolicy,
  otherUserSubmissionAccess: OtherUserSubmissionAccess
)

/** CreateProblemRequest 的 JSON 编解码器。 */
object CreateProblemRequest:
  given Encoder[CreateProblemRequest] = deriveEncoder[CreateProblemRequest]
  given Decoder[CreateProblemRequest] = deriveDecoder[CreateProblemRequest]
