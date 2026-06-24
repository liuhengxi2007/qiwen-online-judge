package domains.auth.objects.internal

import domains.user.objects.Username

/** 已通过 SessionResolver 解析的用户身份，供 AuthenticatedApi/SiteManagerApi 的 plan 和各领域 AccessRules 鉴权使用。 */
final case class AuthenticatedUser(
  username: Username,
  siteManager: Boolean,
  problemManager: Boolean,
  contestManager: Boolean
)
