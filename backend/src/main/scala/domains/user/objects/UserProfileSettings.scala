package domains.user.objects

import domains.problem.objects.ProblemTitleDisplayMode

/** 用户资料设置内部/响应模型，包含展示名、偏好字段和可选头像 URL。 */
final case class UserProfileSettings(
  username: Username,
  displayName: DisplayName,
  displayMode: UserDisplayMode,
  locale: UserLocale,
  problemTitleDisplayMode: ProblemTitleDisplayMode,
  autoMarkMessageRead: Boolean,
  avatarUrl: Option[UserAvatarUrl]
):
  /** 将分散存储的偏好字段组合为前端使用的 UserPreferences。 */
  def preferences: UserPreferences =
    UserPreferences(
      displayMode = displayMode,
      locale = locale,
      problemTitleDisplayMode = problemTitleDisplayMode,
      autoMarkMessageRead = autoMarkMessageRead
    )
