package database.utils

import java.sql.ResultSet



/** SQL 层读取到的用户身份行，稍后由领域层转换为 Username 和 DisplayName。 */
final case class UserIdentityRow(
  username: String,
  displayName: String
)

/** 复用用户身份 SQL 片段和 ResultSet 读取逻辑，避免各领域重复拼接展示名 join。 */
object UserIdentitySql:

  /** 返回必需用户身份列选择片段，输入列名和别名必须来自代码常量。 */
  def selectColumns(usernameColumn: String, alias: String, displayNameTableAlias: String): String =
    s"$usernameColumn, $displayNameTableAlias.display_name as ${alias}_display_name"

  /** 返回可选用户身份列选择片段，目前与必需列形状一致。 */
  def selectOptionalColumns(usernameColumn: String, alias: String, displayNameTableAlias: String): String =
    selectColumns(usernameColumn, alias, displayNameTableAlias)

  /** 构造 user_profiles 内连接 SQL 片段，调用方必须传入可信列名。 */
  def joinUserProfiles(usernameColumn: String, displayNameTableAlias: String): String =
    s"join user_profiles $displayNameTableAlias on $displayNameTableAlias.username = $usernameColumn"

  /** 构造 user_profiles 左连接 SQL 片段，用于允许身份缺失的查询。 */
  def leftJoinUserProfiles(usernameColumn: String, displayNameTableAlias: String): String =
    s"left join user_profiles $displayNameTableAlias on $displayNameTableAlias.username = $usernameColumn"

  /** 构造 returning 子查询列，供插入/更新后返回身份展示信息。 */
  def returningColumns(usernameColumn: String, alias: String): String =
    s"$usernameColumn, (select display_name from user_profiles where username = $usernameColumn) as ${alias}_display_name"

  /** 构造可选 returning 身份列，目前与必需列形状一致。 */
  def returningOptionalColumns(usernameColumn: String, alias: String): String =
    returningColumns(usernameColumn, alias)

  /** 从默认列名读取用户身份行。 */
  def readUserIdentityRow(resultSet: ResultSet): UserIdentityRow =
    UserIdentityRow(
      username = resultSet.getString("username"),
      displayName = resultSet.getString("display_name")
    )

  /** 从带前缀列名读取用户身份行。 */
  def readUserIdentityRow(resultSet: ResultSet, prefix: String): UserIdentityRow =
    UserIdentityRow(
      username = resultSet.getString(s"${prefix}_username"),
      displayName = resultSet.getString(s"${prefix}_display_name")
    )

  /** 从带前缀列名读取可选用户身份，username 为 null 时返回 None。 */
  def readOptionalUserIdentityRow(resultSet: ResultSet, prefix: String): Option[UserIdentityRow] =
    Option(resultSet.getString(s"${prefix}_username")).map { username =>
      UserIdentityRow(
        username = username,
        displayName = resultSet.getString(s"${prefix}_display_name")
      )
    }
