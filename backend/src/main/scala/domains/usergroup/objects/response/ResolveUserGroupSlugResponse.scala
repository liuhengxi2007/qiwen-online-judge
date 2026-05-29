package domains.usergroup.objects.response

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

final case class ResolveUserGroupSlugResponse(
  exists: Boolean
)

object ResolveUserGroupSlugResponse:
  given Encoder[ResolveUserGroupSlugResponse] = deriveEncoder[ResolveUserGroupSlugResponse]
  given Decoder[ResolveUserGroupSlugResponse] = deriveDecoder[ResolveUserGroupSlugResponse]
