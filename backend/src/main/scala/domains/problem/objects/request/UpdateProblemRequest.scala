package domains.problem.objects.request

import domains.problem.objects.*
import domains.user.objects.Username

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import shared.objects.access.ResourceAccessPolicy

/** 更新题目的请求体；覆盖题面、访问策略、他人提交可见性和作者展示账号。 */
final case class UpdateProblemRequest(
  title: ProblemTitle,
  statement: ProblemStatementText,
  accessPolicy: ResourceAccessPolicy,
  otherUserSubmissionAccess: OtherUserSubmissionAccess,
  authorUsername: Option[Username]
)

/** UpdateProblemRequest 的 JSON 编解码器。 */
object UpdateProblemRequest:
  given Encoder[UpdateProblemRequest] = deriveEncoder[UpdateProblemRequest]
  given Decoder[UpdateProblemRequest] = deriveDecoder[UpdateProblemRequest]
