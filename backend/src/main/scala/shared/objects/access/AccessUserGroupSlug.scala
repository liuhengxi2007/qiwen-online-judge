package shared.objects.access

/** 访问控制中引用的用户组 slug，独立于用户组领域对象以降低共享层依赖。 */
final case class AccessUserGroupSlug(value: String)

/** 提供授权用户组 slug 的输入校验，确保共享访问策略不接收非法路径片段。 */
object AccessUserGroupSlug:
  private val slugPattern = "^[a-z0-9]+(?:-[a-z0-9]+)*$".r

  /** 解析用户组 slug，要求小写字母、数字和中划线组成，返回业务校验错误。 */
  def parse(raw: String): Either[String, AccessUserGroupSlug] =
    val normalized = raw.trim
    if normalized.isEmpty then Left("User group slug is required.")
    else if normalized.length < 3 || normalized.length > 64 then Left("User group slug must be between 3 and 64 characters.")
    else if !slugPattern.matches(normalized) then Left("User group slug may contain only lowercase letters, numbers, and hyphens.")
    else Right(AccessUserGroupSlug(normalized))
