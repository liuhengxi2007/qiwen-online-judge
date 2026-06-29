package database.utils



import scala.annotation.tailrec

/** 已转义的 SQL LIKE 查询模式，区分包含匹配和前缀匹配。 */
final case class LikePatternSql(
  raw: String,
  containsPattern: String,
  prefixPattern: String
)

/** 将用户搜索输入转换为带反斜杠转义的 LIKE 模式，防止通配符被误解释。 */
object LikePatternSql:

  /** 从原始搜索词生成 contains/prefix 两类模式，支持 * 和 ? 作为显式通配符。 */
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
