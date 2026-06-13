package domains.user.objects.response

import domains.rating.objects.RatingValue
import domains.user.objects.{DisplayName, UserAcceptedProblem, UserAvatarUrl, UserContribution, Username}
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

/** 用户公开资料响应，包含展示信息、贡献、rating 和已通过题目。 */
final case class UserProfileResponse(
  username: Username,
  displayName: DisplayName,
  avatarUrl: Option[UserAvatarUrl],
  contribution: UserContribution,
  rating: RatingValue,
  acceptedProblems: List[UserAcceptedProblem]
)

/** 提供用户公开资料响应 JSON 编解码。 */
object UserProfileResponse:
  given Encoder[UserProfileResponse] = deriveEncoder[UserProfileResponse]
  given Decoder[UserProfileResponse] = deriveDecoder[UserProfileResponse]
