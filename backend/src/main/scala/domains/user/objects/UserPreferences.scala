package domains.user.objects



import domains.problem.objects.ProblemTitleDisplayMode
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

/** 用户偏好设置，聚合展示模式、语言、题目标题显示和消息已读行为。 */
final case class UserPreferences(
  displayMode: UserDisplayMode,
  locale: UserLocale,
  problemTitleDisplayMode: ProblemTitleDisplayMode,
  autoMarkMessageRead: Boolean
)

/** 提供用户偏好 JSON 编解码，字段形状需与前端镜像类型保持一致。 */
object UserPreferences:
  given Encoder[UserPreferences] = deriveEncoder[UserPreferences]
  given Decoder[UserPreferences] = deriveDecoder[UserPreferences]
