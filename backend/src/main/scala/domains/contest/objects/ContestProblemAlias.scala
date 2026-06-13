package domains.contest.objects

import io.circe.{Decoder, Encoder}

/** 比赛题目别名领域值，通常由题目位置生成 A/B/C 等展示编号。 */
final case class ContestProblemAlias(value: String)

/** 提供比赛题目别名 codec、格式校验和按位置生成别名的逻辑。 */
object ContestProblemAlias:
  given Encoder[ContestProblemAlias] = Encoder.encodeString.contramap(_.value)
  given Decoder[ContestProblemAlias] = Decoder.decodeString.emap(parse)

  private val aliasPattern = "^[A-Z][A-Z0-9]{0,7}$".r

  /** 规范化为大写并要求以字母开头、总长不超过 8。 */
  def parse(raw: String): Either[String, ContestProblemAlias] =
    val normalized = raw.trim.toUpperCase
    if normalized.isEmpty then Left("Contest problem alias is required.")
    else if !aliasPattern.matches(normalized) then Left("Contest problem alias must start with A-Z and contain at most 8 uppercase letters or digits.")
    else Right(ContestProblemAlias(normalized))

  /** 根据一基题目位置生成字母序别名，position 小于 1 时按 1 处理。 */
  def fromPosition(position: Int): ContestProblemAlias =
    ContestProblemAlias(toLetters(position))

  private def toLetters(position: Int): String =
    val zeroBased = math.max(position, 1) - 1
    def loop(value: Int, acc: String): String =
      val letter = ('A' + (value % 26)).toChar.toString
      val next = value / 26 - 1
      if next < 0 then letter + acc else loop(next, letter + acc)
    loop(zeroBased, "")
