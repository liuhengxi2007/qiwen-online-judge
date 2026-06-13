package domains.auth.objects

/** 已哈希的密码字符串，只应由 PasswordHasher 生成并持久化。 */
final case class PasswordHash(value: String)
