package shared.objects.access

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

/** 资源访问策略，包含默认可见性以及显式授予查看/管理权限的主体列表。 */
final case class ResourceAccessPolicy(
  baseAccess: BaseAccess,
  viewerGrants: List[AccessSubject],
  managerGrants: List[AccessSubject]
)

/** 提供资源访问策略的 JSON 编解码，字段形状需与前端镜像类型保持一致。 */
object ResourceAccessPolicy:
  given Encoder[ResourceAccessPolicy] = deriveEncoder[ResourceAccessPolicy]
  given Decoder[ResourceAccessPolicy] = deriveDecoder[ResourceAccessPolicy]
