package domains.contest.objects

import io.circe.{Decoder, Encoder}

/** 比赛 URL slug 领域值，用于公开路径和内部查找。 */
final case class ContestSlug(value: String)

/** 提供比赛 slug JSON codec 和路径安全格式校验。 */
object ContestSlug:
  given Encoder[ContestSlug] = Encoder.encodeString.contramap(_.value)
  given Decoder[ContestSlug] = Decoder.decodeString.emap(parse)

  private val slugPattern = "^[a-z0-9]+(?:-[a-z0-9]+)*$".r

  /** 去除首尾空白，要求 3-64 位小写字母、数字和中横线组合。 */
  def parse(raw: String): Either[String, ContestSlug] =
    val normalized = raw.trim
    if normalized.isEmpty then Left("Contest slug is required.")
    else if normalized.length < 3 || normalized.length > 64 then Left("Contest slug must be between 3 and 64 characters.")
    else if !slugPattern.matches(normalized) then Left("Contest slug may contain only lowercase letters, numbers, and hyphens.")
    else Right(ContestSlug(normalized))
