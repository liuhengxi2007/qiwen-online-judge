package domains.auth.utils

/** 账号领域的硬性规则，集中声明受保护账号等跨 API 常量。 */
object AuthAccountRules:
  val protectedAdminUsername: String = "admin"
