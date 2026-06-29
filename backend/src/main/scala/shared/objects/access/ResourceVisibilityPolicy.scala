package shared.objects.access

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

/** 资源可见性策略，只包含默认可见性以及显式授予查看权限的主体列表。 */
final case class ResourceVisibilityPolicy(
  baseAccess: BaseAccess,
  viewerGrants: List[AccessSubject]
)

/** 提供资源可见性策略的 JSON 编解码，字段形状需与前端镜像类型保持一致。 */
object ResourceVisibilityPolicy:
  given Encoder[ResourceVisibilityPolicy] = deriveEncoder[ResourceVisibilityPolicy]
  given Decoder[ResourceVisibilityPolicy] = deriveDecoder[ResourceVisibilityPolicy]
