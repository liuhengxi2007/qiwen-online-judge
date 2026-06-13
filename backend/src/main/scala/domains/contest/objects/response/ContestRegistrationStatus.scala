package domains.contest.objects.response

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

/** 当前用户相对比赛的报名状态响应。 */
final case class ContestRegistrationStatus(
  isRegistered: Boolean
)

/** 提供报名状态常用值和 JSON codec。 */
object ContestRegistrationStatus:
  val notRegistered: ContestRegistrationStatus =
    ContestRegistrationStatus(isRegistered = false)

  val registered: ContestRegistrationStatus =
    ContestRegistrationStatus(isRegistered = true)

  given Encoder[ContestRegistrationStatus] = deriveEncoder[ContestRegistrationStatus]
  given Decoder[ContestRegistrationStatus] = deriveDecoder[ContestRegistrationStatus]
