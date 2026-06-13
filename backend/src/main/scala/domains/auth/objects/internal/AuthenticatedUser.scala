package domains.auth.objects.internal

import domains.user.objects.Username

/** 已通过会话解析的用户身份，作为所有登录态 API 的权限输入。 */
final case class AuthenticatedUser(
  username: Username,
  siteManager: Boolean,
  problemManager: Boolean,
  contestManager: Boolean
)
