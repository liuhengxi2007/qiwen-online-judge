package domains.shared.access



import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}

final case class ResourceAccessPolicy(
  baseAccess: BaseAccess,
  viewerGrants: List[AccessSubject],
  managerGrants: List[AccessSubject]
)

object ResourceAccessPolicy:
  given Encoder[ResourceAccessPolicy] = deriveEncoder[ResourceAccessPolicy]
  given Decoder[ResourceAccessPolicy] = deriveDecoder[ResourceAccessPolicy]
