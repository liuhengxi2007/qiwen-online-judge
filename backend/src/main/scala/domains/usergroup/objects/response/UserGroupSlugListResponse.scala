package domains.usergroup.objects.response

import domains.usergroup.objects.UserGroupSlug
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

final case class UserGroupSlugListResponse(
  slugs: List[UserGroupSlug]
)

object UserGroupSlugListResponse:
  given Encoder[UserGroupSlugListResponse] = deriveEncoder[UserGroupSlugListResponse]
  given Decoder[UserGroupSlugListResponse] = deriveDecoder[UserGroupSlugListResponse]
