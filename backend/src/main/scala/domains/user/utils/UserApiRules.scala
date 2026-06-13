package domains.user.utils

/** 用户 API 的固定业务规则，集中声明榜单页大小和建议查询阈值。 */
object UserApiRules:
  val ranklistPageSize: Int = 10
  val minSuggestionQueryLength: Int = 1
