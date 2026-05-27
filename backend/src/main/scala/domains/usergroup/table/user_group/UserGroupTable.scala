package domains.usergroup.table.user_group



import cats.effect.IO
import domains.auth.objects.AuthUser
import domains.user.objects.{Username}
import shared.objects.PageResponse
import domains.usergroup.objects.request.{AddUserGroupMemberRequest, CreateUserGroupRequest, UpdateUserGroupRequest}
import domains.usergroup.objects.{UserGroup, UserGroupId, UserGroupMember, UserGroupRole, UserGroupSlug}
import domains.usergroup.objects.response.{UserGroupSummary}
import domains.usergroup.table.user_group.UserGroupTableSchema.*
import domains.usergroup.table.user_group.UserGroupTableSupport.*

import java.sql.{Connection, SQLException, Timestamp}
import java.time.Instant
import java.util.UUID

object UserGroupTable:

  enum AddMemberTableResult:
    case AlreadyExists
    case Added
    case UserNotFound

  enum UpdateMemberRoleTableResult:
    case MemberNotFound
    case Updated

  enum RemoveMemberTableResult:
    case MemberNotFound
    case Removed

  def initialize(connection: Connection): IO[Unit] =
    UserGroupTableSchema.initialize(connection)

  private val countVisibleSQL: String =
    """
      |select count(*) as total_items
      |from user_groups ug
      |where
      |  ? = true
      |  or lower(ug.owner_username) = lower(?)
      |  or exists (
      |    select 1
      |    from user_group_memberships ugm
      |    where ugm.user_group_id = ug.id and lower(ugm.username) = lower(?)
      |  )
      |""".stripMargin

  private val listVisibleSQL: String =
    """
      |select ug.id, ug.slug, ug.name, ug.description, ug.owner_username, ug.created_at, ug.updated_at
      |from user_groups ug
      |where
      |  ? = true
      |  or lower(ug.owner_username) = lower(?)
      |  or exists (
      |    select 1
      |    from user_group_memberships ugm
      |    where ugm.user_group_id = ug.id and lower(ugm.username) = lower(?)
      |  )
      |order by ug.updated_at desc, ug.slug asc
      |limit ? offset ?
      |""".stripMargin

  def listVisibleTo(connection: Connection, actor: AuthUser, page: Int, pageSize: Int): IO[PageResponse[UserGroupSummary]] =
    for
      totalItems <- IO.blocking {
        val statement = connection.prepareStatement(countVisibleSQL)
        try
          statement.setBoolean(1, actor.siteManager)
          statement.setString(2, actor.username.value)
          statement.setString(3, actor.username.value)
          val resultSet = statement.executeQuery()
          try if resultSet.next() then resultSet.getLong("total_items") else 0L
          finally resultSet.close()
        finally statement.close()
      }
      items <- IO.blocking {
        val statement = connection.prepareStatement(listVisibleSQL)
        try
          statement.setBoolean(1, actor.siteManager)
          statement.setString(2, actor.username.value)
          statement.setString(3, actor.username.value)
          statement.setInt(4, pageSize)
          statement.setInt(5, (page - 1) * pageSize)
          val resultSet = statement.executeQuery()
          try Iterator.continually(resultSet.next()).takeWhile(identity).map(_ => readSummary(resultSet)).toList
          finally resultSet.close()
        finally statement.close()
      }
    yield PageResponse(items = items, page = page, pageSize = pageSize, totalItems = totalItems)

  private val listMembersSQL: String =
    """
      |select ugm.username, au.display_name, ugm.role, ugm.joined_at
      |from user_group_memberships ugm
      |join auth_users au on au.username = ugm.username
      |where ugm.user_group_id = ?
      |order by
      |  case ugm.role
      |    when 'owner' then 1
      |    when 'manager' then 2
      |    else 3
      |  end asc,
      |  lower(ugm.username) asc
      |""".stripMargin

  private def listMembers(connection: java.sql.Connection, groupId: UserGroupId): IO[List[UserGroupMember]] =
    IO.blocking {
      val statement = connection.prepareStatement(listMembersSQL)
      try
        statement.setObject(1, groupId.value)
        val resultSet = statement.executeQuery()
        try Iterator.continually(resultSet.next()).takeWhile(identity).map(_ => readMember(resultSet)).toList
        finally resultSet.close()
      finally statement.close()
    }

  private val findBySlugSQL: String =
    """
      |select id, slug, name, description, owner_username, created_at, updated_at
      |from user_groups
      |where slug = ?
      |""".stripMargin

  def findBySlug(connection: Connection, slug: UserGroupSlug): IO[Option[UserGroup]] =
    IO.blocking {
      val statement = connection.prepareStatement(findBySlugSQL)
      try
        statement.setString(1, slug.value)
        val resultSet = statement.executeQuery()
        try if resultSet.next() then Some(readDetailBase(resultSet)) else None
        finally resultSet.close()
      finally statement.close()
    }.flatMap {
      case Some(group) => listMembers(connection, group.id).map(members => Some(group.copy(members = members)))
      case None => IO.pure(None)
    }

  private val listGroupSlugsForMemberSQL: String =
    """
      |select ug.slug
      |from user_group_memberships ugm
      |join user_groups ug on ug.id = ugm.user_group_id
      |where ugm.username = ?
      |order by ug.slug asc
      |""".stripMargin

  def listGroupSlugsForMember(connection: Connection, username: Username): IO[Set[UserGroupSlug]] =
    IO.blocking {
      val statement = connection.prepareStatement(listGroupSlugsForMemberSQL)
      try
        statement.setString(1, username.value)
        val resultSet = statement.executeQuery()
        try
          Iterator
            .continually(resultSet.next())
            .takeWhile(identity)
            .map(_ => parseColumn("user_groups.slug", resultSet.getString("slug"), UserGroupSlug.parse))
            .toSet
        finally resultSet.close()
      finally statement.close()
    }

  private val insertSQL: String =
    """
      |insert into user_groups (id, slug, name, description, visibility, owner_username, created_at, updated_at)
      |values (?, ?, ?, ?, ?, ?, ?, ?)
      |returning id, slug, name, description, owner_username, created_at, updated_at
      |""".stripMargin

  private val insertOwnerMembershipSQL: String =
    """
      |insert into user_group_memberships (user_group_id, username, role, joined_at)
      |values (?, ?, 'owner', ?)
      |""".stripMargin

  def insert(connection: Connection, ownerUsername: Username, request: CreateUserGroupRequest): IO[UserGroup] =
    IO.blocking {
      val now = Instant.now()
      val groupId = UserGroupId(UUID.randomUUID())
      val statement = connection.prepareStatement(insertSQL)
      try
        statement.setObject(1, groupId.value)
        statement.setString(2, request.slug.value)
        statement.setString(3, request.name.value)
        statement.setString(4, request.description.value)
        statement.setString(5, "private")
        statement.setString(6, ownerUsername.value)
        statement.setTimestamp(7, Timestamp.from(now))
        statement.setTimestamp(8, Timestamp.from(now))
        val resultSet = statement.executeQuery()
        try
          if resultSet.next() then
            val ownerMembershipStatement = connection.prepareStatement(insertOwnerMembershipSQL)
            try
              ownerMembershipStatement.setObject(1, groupId.value)
              ownerMembershipStatement.setString(2, ownerUsername.value)
              ownerMembershipStatement.setTimestamp(3, Timestamp.from(now))
              ownerMembershipStatement.executeUpdate()
            finally ownerMembershipStatement.close()
            readDetailBase(resultSet)
          else missingInsertResult("user group")
        finally resultSet.close()
      finally statement.close()
    }.flatMap { group =>
      listMembers(connection, group.id).map(members => group.copy(members = members))
    }

  private val updateSQL: String =
    """
      |update user_groups
      |set name = ?, description = ?, updated_at = ?
      |where id = ?
      |""".stripMargin

  def update(connection: Connection, groupId: UserGroupId, request: UpdateUserGroupRequest): IO[Unit] =
    IO.blocking {
      val statement = connection.prepareStatement(updateSQL)
      try
        statement.setString(1, request.name.value)
        statement.setString(2, request.description.value)
        statement.setTimestamp(3, Timestamp.from(Instant.now()))
        statement.setObject(4, groupId.value)
        statement.executeUpdate()
        ()
      finally statement.close()
    }

  private val deleteSQL: String =
    """
      |delete from user_groups
      |where id = ?
      |""".stripMargin

  def delete(connection: Connection, groupId: UserGroupId): IO[Unit] =
    IO.blocking {
      val statement = connection.prepareStatement(deleteSQL)
      try
        statement.setObject(1, groupId.value)
        statement.executeUpdate()
        ()
      finally statement.close()
    }

  private val userExistsSQL: String =
    """
      |select 1
      |from auth_users
      |where username = ?
      |""".stripMargin

  def userExists(connection: Connection, username: Username): IO[Boolean] =
    IO.blocking {
      val statement = connection.prepareStatement(userExistsSQL)
      try
        statement.setString(1, username.value)
        val resultSet = statement.executeQuery()
        try resultSet.next()
        finally resultSet.close()
      finally statement.close()
    }

  private val membershipExistsSQL: String =
    """
      |select 1
      |from user_group_memberships
      |where user_group_id = ? and username = ?
      |""".stripMargin

  private val addMemberSQL: String =
    """
      |insert into user_group_memberships (user_group_id, username, role, joined_at)
      |values (?, ?, ?, ?)
      |""".stripMargin

  def addMember(connection: Connection, groupId: UserGroupId, request: AddUserGroupMemberRequest): IO[AddMemberTableResult] =
    for
      membershipExists <- IO.blocking {
        val statement = connection.prepareStatement(membershipExistsSQL)
        try
          statement.setObject(1, groupId.value)
          statement.setString(2, request.username.value)
          val resultSet = statement.executeQuery()
          try resultSet.next()
          finally resultSet.close()
        finally statement.close()
      }
      result <- if membershipExists then
        IO.pure(AddMemberTableResult.AlreadyExists)
      else
        IO.blocking {
          val statement = connection.prepareStatement(addMemberSQL)
          try
            statement.setObject(1, groupId.value)
            statement.setString(2, request.username.value)
            statement.setString(3, encodeAddUserGroupMemberRoleColumn(request.role))
            statement.setTimestamp(4, Timestamp.from(Instant.now()))
            statement.executeUpdate()
            AddMemberTableResult.Added
          catch
            case exception: SQLException if exception.getSQLState == "23503" =>
              AddMemberTableResult.UserNotFound
            case exception: SQLException if exception.getSQLState == "23505" =>
              AddMemberTableResult.AlreadyExists
          finally statement.close()
        }
    yield result

  private val updateMemberRoleSQL: String =
    """
      |update user_group_memberships
      |set role = ?
      |where user_group_id = ? and username = ?
      |""".stripMargin

  def updateMemberRole(
    connection: Connection,
    groupId: UserGroupId,
    targetUsername: Username,
    role: UserGroupRole
  ): IO[UpdateMemberRoleTableResult] =
    IO.blocking {
      val statement = connection.prepareStatement(updateMemberRoleSQL)
      try
        statement.setString(1, encodeUserGroupRoleColumn(role))
        statement.setObject(2, groupId.value)
        statement.setString(3, targetUsername.value)
        val updatedRows = statement.executeUpdate()
        if updatedRows == 0 then UpdateMemberRoleTableResult.MemberNotFound else UpdateMemberRoleTableResult.Updated
      finally statement.close()
    }

  private val updateOwnerUsernameSQL: String =
    """
      |update user_groups
      |set owner_username = ?, updated_at = ?
      |where id = ?
      |""".stripMargin

  def transferOwnership(
    connection: Connection,
    groupId: UserGroupId,
    currentOwnerUsername: Username,
    newOwnerUsername: Username
  ): IO[UpdateMemberRoleTableResult] =
    updateMemberRole(connection, groupId, currentOwnerUsername, UserGroupRole.Manager).flatMap {
      case UpdateMemberRoleTableResult.MemberNotFound =>
        IO.pure(UpdateMemberRoleTableResult.MemberNotFound)
      case UpdateMemberRoleTableResult.Updated =>
        updateMemberRole(connection, groupId, newOwnerUsername, UserGroupRole.Owner).flatMap {
          case UpdateMemberRoleTableResult.MemberNotFound =>
            IO.pure(UpdateMemberRoleTableResult.MemberNotFound)
          case UpdateMemberRoleTableResult.Updated =>
            IO.blocking {
              val statement = connection.prepareStatement(updateOwnerUsernameSQL)
              try
                statement.setString(1, newOwnerUsername.value)
                statement.setTimestamp(2, Timestamp.from(Instant.now()))
                statement.setObject(3, groupId.value)
                statement.executeUpdate()
                UpdateMemberRoleTableResult.Updated
              finally statement.close()
            }
        }
    }

  private val deleteMemberSQL: String =
    """
      |delete from user_group_memberships
      |where user_group_id = ? and username = ?
      |""".stripMargin

  def removeMember(
    connection: Connection,
    groupId: UserGroupId,
    targetUsername: Username
  ): IO[RemoveMemberTableResult] =
    IO.blocking {
      val statement = connection.prepareStatement(deleteMemberSQL)
      try
        statement.setObject(1, groupId.value)
        statement.setString(2, targetUsername.value)
        val deletedRows = statement.executeUpdate()
        if deletedRows == 0 then RemoveMemberTableResult.MemberNotFound else RemoveMemberTableResult.Removed
      finally statement.close()
    }
