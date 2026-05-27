package domains.usergroup.table.user_group



import domains.user.objects.{DisplayName, Username}
import domains.usergroup.objects.{AddUserGroupMemberRole, UserGroup, UserGroupDescription, UserGroupId, UserGroupMember, UserGroupName, UserGroupRole, UserGroupSlug}
import domains.usergroup.objects.response.{UserGroupSummary}

import java.sql.ResultSet

object UserGroupTableSupport:

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

  def readMember(resultSet: ResultSet): UserGroupMember =
    UserGroupMember(
      username = Username.canonical(resultSet.getString("username")),
      displayName = DisplayName(resultSet.getString("display_name")),
      role = parseOptionalColumn("user_group_memberships.role", resultSet.getString("role"), decodeUserGroupRoleColumn),
      joinedAt = resultSet.getTimestamp("joined_at").toInstant
    )

  def encodeAddUserGroupMemberRoleColumn(value: AddUserGroupMemberRole): String =
    value match
      case AddUserGroupMemberRole.Manager => "manager"
      case AddUserGroupMemberRole.Member => "member"

  def encodeUserGroupRoleColumn(value: UserGroupRole): String =
    value match
      case UserGroupRole.Owner => "owner"
      case UserGroupRole.Manager => "manager"
      case UserGroupRole.Member => "member"

  def decodeUserGroupRoleColumn(value: String): Option[UserGroupRole] =
    UserGroupRole.parse(value).toOption

  def parseColumn[A](columnName: String, rawValue: String, parse: String => Either[String, A]): A =
    parse(rawValue).fold(message => throw IllegalStateException(s"Invalid value in $columnName: $message"), identity)

  def parseOptionalColumn[A](columnName: String, rawValue: String, parse: String => Option[A]): A =
    parse(rawValue).getOrElse(throw IllegalStateException(s"Invalid value in $columnName: $rawValue"))

  def missingInsertResult(entityName: String): Nothing =
    throw new IllegalStateException(s"Insert succeeded but returned no $entityName")
