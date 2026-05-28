package shared.objects.access

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

final case class ResourceAccessPolicy(
  baseAccess: BaseAccess,
  viewerGrants: List[AccessSubject],
  managerGrants: List[AccessSubject]
)

object ResourceAccessPolicy:
  given Encoder[ResourceAccessPolicy] = deriveEncoder[ResourceAccessPolicy]
  given Decoder[ResourceAccessPolicy] = deriveDecoder[ResourceAccessPolicy]
