package domains.problem.objects

import io.circe.{Decoder, Encoder}


/** 题目公开 slug；可用于 URL、搜索和跨域引用，必须是小写短横线格式。 */
final case class ProblemSlug(value: String)

/** ProblemSlug 的 JSON 编解码与格式校验入口。 */
object ProblemSlug:
  given Encoder[ProblemSlug] = Encoder.encodeString.contramap(_.value)
  given Decoder[ProblemSlug] = Decoder.decodeString.emap(parse)

  private val slugPattern = "^[a-z0-9]+(?:-[a-z0-9]+)*$".r

  /** 校验 slug 长度和字符集，输出规范化的小写 slug。 */
  def parse(raw: String): Either[String, ProblemSlug] =
    val normalized = raw.trim
    if normalized.isEmpty then Left("Problem slug is required.")
    else if normalized.length < 3 || normalized.length > 64 then Left("Problem slug must be between 3 and 64 characters.")
    else if !slugPattern.matches(normalized) then Left("Problem slug may contain only lowercase letters, numbers, and hyphens.")
    else Right(ProblemSlug(normalized))
