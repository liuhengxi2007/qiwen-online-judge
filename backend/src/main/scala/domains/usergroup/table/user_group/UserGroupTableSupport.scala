package domains.usergroup.table.user_group



import domains.user.objects.{DisplayName, Username}
import domains.usergroup.objects.{UserGroup, UserGroupDescription, UserGroupId, UserGroupMember, UserGroupName, UserGroupRole, UserGroupSlug}
import domains.usergroup.objects.request.NewUserGroupMemberRole
import domains.usergroup.objects.response.{UserGroupSummary}

import java.sql.ResultSet

/** 用户组表 ResultSet 读取、角色列编解码和表层异常辅助。 */
object UserGroupTableSupport:

  /** 从当前行读取用户组摘要。 */
  def readSummary(resultSet: ResultSet): UserGroupSummary =
    UserGroupSummary(
      id = UserGroupId(resultSet.getObject("id", classOf[java.util.UUID])),
      slug = parseColumn("user_groups.slug", resultSet.getString("slug"), UserGroupSlug.parse),
      name = parseColumn("user_groups.name", resultSet.getString("name"), UserGroupName.parse),
      description = parseColumn("user_groups.description", resultSet.getString("description"), UserGroupDescription.parse),
      ownerUsername = Username.canonical(resultSet.getString("owner_username")),
      createdAt = resultSet.getTimestamp("created_at").toInstant,
      updatedAt = resultSet.getTimestamp("updated_at").toInstant
    )

  /** 从当前行读取不含成员列表的用户组详情基础信息。 */
  def readDetailBase(resultSet: ResultSet): UserGroup =
    UserGroup(
      id = UserGroupId(resultSet.getObject("id", classOf[java.util.UUID])),
      slug = parseColumn("user_groups.slug", resultSet.getString("slug"), UserGroupSlug.parse),
      name = parseColumn("user_groups.name", resultSet.getString("name"), UserGroupName.parse),
      description = parseColumn("user_groups.description", resultSet.getString("description"), UserGroupDescription.parse),
      ownerUsername = Username.canonical(resultSet.getString("owner_username")),
      members = Nil,
      createdAt = resultSet.getTimestamp("created_at").toInstant,
      updatedAt = resultSet.getTimestamp("updated_at").toInstant
    )

  /** 从当前行读取用户组成员，角色列非法时抛出状态异常。 */
  def readMember(resultSet: ResultSet): UserGroupMember =
    UserGroupMember(
      username = Username.canonical(resultSet.getString("username")),
      displayName = DisplayName(resultSet.getString("display_name")),
      role = parseOptionalColumn("user_group_memberships.role", resultSet.getString("role"), decodeUserGroupRoleColumn),
      joinedAt = resultSet.getTimestamp("joined_at").toInstant
    )

  /** 将新增成员允许角色编码为数据库列值。 */
  def encodeNewUserGroupMemberRoleColumn(value: NewUserGroupMemberRole): String =
    value match
      case NewUserGroupMemberRole.Manager => "manager"
      case NewUserGroupMemberRole.Member => "member"

  /** 将完整用户组角色编码为数据库列值。 */
  def encodeUserGroupRoleColumn(value: UserGroupRole): String =
    value match
      case UserGroupRole.Owner => "owner"
      case UserGroupRole.Manager => "manager"
      case UserGroupRole.Member => "member"

  /** 从数据库列值解码用户组角色。 */
  def decodeUserGroupRoleColumn(value: String): Option[UserGroupRole] =
    UserGroupRole.parse(value).toOption

  /** 解析数据库字符串列，非法值说明数据库状态异常并抛出异常。 */
  def parseColumn[A](columnName: String, rawValue: String, parse: String => Either[String, A]): A =
    parse(rawValue).fold(message => throw IllegalStateException(s"Invalid value in $columnName: $message"), identity)

  /** 解析返回 Option 的数据库字符串列，非法值说明数据库状态异常并抛出异常。 */
  def parseOptionalColumn[A](columnName: String, rawValue: String, parse: String => Option[A]): A =
    parse(rawValue).getOrElse(throw IllegalStateException(s"Invalid value in $columnName: $rawValue"))

  /** 表示 insert returning 没有返回行的异常边界，说明数据库状态不符合预期。 */
  def missingInsertResult(entityName: String): Nothing =
    throw new IllegalStateException(s"Insert succeeded but returned no $entityName")
