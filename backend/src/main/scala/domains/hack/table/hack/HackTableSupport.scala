package domains.hack.table.hack

import database.utils.UserIdentitySql
import domains.hack.objects.HackStatus
import domains.user.objects.{DisplayName, UserIdentity, Username}
import io.circe.parser.decode
import io.circe.syntax.*
import judgeprotocol.objects.response.JudgeResult

import java.sql.{PreparedStatement, ResultSet}

/** hack 表读写辅助；集中处理状态列编解码和用户身份映射。 */
object HackTableSupport:

  /** 将 hack 状态编码为数据库列值。 */
  def encodeHackStatusColumn(value: HackStatus): String =
    HackStatus.encode(value)

  /** 从数据库列解析 hack 状态；非法值视为数据损坏。 */
  def decodeHackStatusColumn(value: String): HackStatus =
    HackStatus.parse(value).fold(message => throw IllegalStateException(s"Invalid hack status: $message"), identity)

  /** 绑定可空 hack judge_result JSON；Some 会序列化为 jsonb 文本。 */
  def setOptionalJudgeResult(
    statement: PreparedStatement,
    parameterIndex: Int,
    value: Option[JudgeResult]
  ): Unit =
    value match
      case Some(currentValue) => statement.setString(parameterIndex, currentValue.asJson.noSpaces)
      case None => statement.setNull(parameterIndex, java.sql.Types.VARCHAR)

  /** 从 JSON 文本列读取可空 hack JudgeResult；非法 JSON 视为数据损坏。 */
  def readOptionalJudgeResult(resultSet: ResultSet, columnName: String): Option[JudgeResult] =
    Option(resultSet.getString(columnName)).map { raw =>
      decode[JudgeResult](raw).fold(error => throw IllegalStateException(s"Invalid hack judge result JSON: ${error.getMessage}"), identity)
    }

  /** 从带前缀的 ResultSet 列读取用户身份。 */
  def readUserIdentity(resultSet: ResultSet, prefix: String): UserIdentity =
    val row = UserIdentitySql.readUserIdentityRow(resultSet, prefix)
    UserIdentity(
      username = Username.canonical(row.username),
      displayName = DisplayName(row.displayName)
    )
