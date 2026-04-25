package domains.shared.sql

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
    val builder = new StringBuilder
    var index = 0

    while index < raw.length do
      raw.charAt(index) match
        case '\\' =>
          if index + 1 < raw.length then
            appendLiteral(builder, raw.charAt(index + 1))
            index += 2
          else
            appendLiteral(builder, '\\')
            index += 1
        case '*' =>
          builder.append('%')
          index += 1
        case '?' =>
          builder.append('_')
          index += 1
        case other =>
          appendLiteral(builder, other)
          index += 1

    builder.toString()

  private def appendLiteral(builder: StringBuilder, character: Char): Unit =
    if character == '%' || character == '_' || character == '\\' then
      builder.append('\\')
    builder.append(character)
