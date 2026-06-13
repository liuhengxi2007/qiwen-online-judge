package domains.contest.objects.response

import domains.user.objects.UserIdentity
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

/** 比赛报名用户响应项，仅暴露用户身份信息。 */
final case class ContestRegistrant(
  user: UserIdentity
)

/** 提供比赛报名用户响应 JSON codec。 */
object ContestRegistrant:
  given Encoder[ContestRegistrant] = deriveEncoder[ContestRegistrant]
  given Decoder[ContestRegistrant] = deriveDecoder[ContestRegistrant]
