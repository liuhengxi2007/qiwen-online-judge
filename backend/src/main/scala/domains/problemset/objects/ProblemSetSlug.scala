package domains.problemset.objects

import io.circe.{Decoder, Encoder}


/** 题单 URL slug 领域值，用于公开路径和内部查找。 */
final case class ProblemSetSlug(value: String)

/** 提供题单 slug JSON codec 和路径安全格式校验。 */
object ProblemSetSlug:
  given Encoder[ProblemSetSlug] = Encoder.encodeString.contramap(_.value)
  given Decoder[ProblemSetSlug] = Decoder.decodeString.emap(parse)

  private val slugPattern = "^[a-z0-9]+(?:-[a-z0-9]+)*$".r

  /** 去除首尾空白，要求 3-64 位小写字母、数字和中横线组合。 */
  def parse(raw: String): Either[String, ProblemSetSlug] =
    val normalized = raw.trim
    if normalized.isEmpty then Left("Problem set slug is required.")
    else if normalized.length < 3 || normalized.length > 64 then Left("Problem set slug must be between 3 and 64 characters.")
    else if !slugPattern.matches(normalized) then Left("Problem set slug may contain only lowercase letters, numbers, and hyphens.")
    else Right(ProblemSetSlug(normalized))
