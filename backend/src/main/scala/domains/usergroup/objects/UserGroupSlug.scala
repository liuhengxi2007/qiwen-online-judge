package domains.usergroup.objects

import io.circe.{Decoder, Encoder}


/** 用户组 slug 值对象，用于 URL、唯一性约束和跨领域引用。 */
final case class UserGroupSlug(value: String)

/** 提供用户组 slug JSON 编解码和格式校验。 */
object UserGroupSlug:
  given Encoder[UserGroupSlug] = Encoder.encodeString.contramap(_.value)
  given Decoder[UserGroupSlug] = Decoder.decodeString.emap(parse)

  private val slugPattern = "^[a-z0-9]+(?:-[a-z0-9]+)*$".r

  /** 解析用户组 slug，要求小写字母、数字和中划线组成。 */
  def parse(raw: String): Either[String, UserGroupSlug] =
    val normalized = raw.trim
    if normalized.isEmpty then Left("User group slug is required.")
    else if normalized.length < 3 || normalized.length > 64 then Left("User group slug must be between 3 and 64 characters.")
    else if !slugPattern.matches(normalized) then Left("User group slug may contain only lowercase letters, numbers, and hyphens.")
    else Right(UserGroupSlug(normalized))
