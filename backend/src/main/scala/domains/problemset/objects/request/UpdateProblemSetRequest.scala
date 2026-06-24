package domains.problemset.objects.request

import domains.problemset.objects.*
import domains.user.objects.Username

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import shared.objects.access.ResourceVisibilityPolicy

/** 更新题单请求体，包含基础信息、访问策略和可选作者引用。 */
final case class UpdateProblemSetRequest(
  title: ProblemSetTitle,
  description: ProblemSetDescription,
  accessPolicy: ResourceVisibilityPolicy,
  authorUsername: Option[Username]
)

/** 提供更新题单请求体 JSON codec。 */
object UpdateProblemSetRequest:
  given Encoder[UpdateProblemSetRequest] = deriveEncoder[UpdateProblemSetRequest]
  given Decoder[UpdateProblemSetRequest] = deriveDecoder[UpdateProblemSetRequest]
