package domains.rating.objects.response

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

final case class RatingManageState(
  contests: List[RatingContestListItem]
)

object RatingManageState:
  given Encoder[RatingManageState] = deriveEncoder[RatingManageState]
  given Decoder[RatingManageState] = deriveDecoder[RatingManageState]
