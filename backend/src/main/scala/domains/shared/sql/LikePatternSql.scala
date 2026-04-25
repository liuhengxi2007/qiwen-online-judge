package domains.shared.sql

import scala.annotation.tailrec

final case class LikePatternSql(
  raw: String,
  containsPattern: String,
  prefixPattern: String
)

object LikePatternSql:

  def fromRaw(raw: String): LikePatternSql =
    val normalizedRaw = raw.trim
    val wildcardPattern = compileWildcardPattern(normalizedRaw)
    LikePatternSql(
      raw = normalizedRaw,
      containsPattern = s"%$wildcardPattern%",
      prefixPattern = s"$wildcardPattern%"
    )

  private def compileWildcardPattern(raw: String): String =
    @tailrec
    def go(remaining: List[Char], acc: List[String]): List[String] =
      remaining match
        case '\\' :: escaped :: tail =>
          go(tail, escapeLiteral(escaped) :: acc)
        case '\\' :: Nil =>
          go(Nil, escapeLiteral('\\') :: acc)
        case '*' :: tail =>
          go(tail, "%" :: acc)
        case '?' :: tail =>
          go(tail, "_" :: acc)
        case character :: tail =>
          go(tail, escapeLiteral(character) :: acc)
        case Nil =>
          acc.reverse

    go(raw.toList, Nil).mkString

  private def escapeLiteral(character: Char): String =
    if character == '%' || character == '_' || character == '\\' then
      s"\\$character"
    else
      character.toString
