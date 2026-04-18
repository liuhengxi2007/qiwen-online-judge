package domains.usergroup.table

import cats.effect.IO
import domains.auth.model.{DisplayName, UserDisplayMode, UserPreferences, Username}
import domains.usergroup.model.{UserGroup, UserGroupDescription, UserGroupId, UserGroupMember, UserGroupName, UserGroupRole, UserGroupSlug, UserGroupSummary}

import java.sql.ResultSet

object UserGroupTableSupport:

  def listMembers(connection: java.sql.Connection, groupId: UserGroupId, listMembersSql: String): IO[List[UserGroupMember]] =
    IO.blocking {
      val statement = connection.prepareStatement(listMembersSql)
      try
        statement.setObject(1, groupId.value)
        val resultSet = statement.executeQuery()
        try Iterator.continually(resultSet.next()).takeWhile(identity).map(_ => readMember(resultSet)).toList
        finally resultSet.close()
      finally statement.close()
    }

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
      preferences =
        UserPreferences(
          displayMode =
            UserDisplayMode
              .fromDatabase(resultSet.getString("display_mode"))
              .getOrElse(throw new IllegalStateException("Invalid user_groups.display_mode."))
        ),
      role = parseOptionalColumn("user_group_memberships.role", resultSet.getString("role"), UserGroupRole.fromDatabase),
      joinedAt = resultSet.getTimestamp("joined_at").toInstant
    )

  def parseColumn[A](columnName: String, rawValue: String, parse: String => Either[String, A]): A =
    parse(rawValue).fold(message => throw IllegalStateException(s"Invalid value in $columnName: $message"), identity)

  def parseOptionalColumn[A](columnName: String, rawValue: String, parse: String => Option[A]): A =
    parse(rawValue).getOrElse(throw IllegalStateException(s"Invalid value in $columnName: $rawValue"))

  def missingInsertResult(entityName: String): Nothing =
    throw new IllegalStateException(s"Insert succeeded but returned no $entityName")
