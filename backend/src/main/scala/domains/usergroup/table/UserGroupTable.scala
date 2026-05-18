package domains.usergroup.table

import cats.effect.IO
import domains.auth.model.{AuthUser, DisplayName, Username}
import domains.shared.model.PageResponse
import domains.usergroup.model.{AddUserGroupMemberRequest, AddUserGroupMemberRole, CreateUserGroupRequest, UpdateUserGroupRequest, UserGroup, UserGroupDescription, UserGroupId, UserGroupMember, UserGroupName, UserGroupRole, UserGroupSlug, UserGroupSummary}
import domains.usergroup.table.UserGroupTableSchema.*
import domains.usergroup.table.UserGroupTableSql.*
import domains.usergroup.table.UserGroupTableSupport.*

import java.sql.{Connection, ResultSet, SQLException, Timestamp}
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

  def listVisibleTo(connection: Connection, actor: AuthUser, page: Int, pageSize: Int): IO[PageResponse[UserGroupSummary]] =
    for
      totalItems <- IO.blocking {
        val statement = connection.prepareStatement(countVisibleSql)
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
        val statement = connection.prepareStatement(listVisibleSql)
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

  def findBySlug(connection: Connection, slug: UserGroupSlug): IO[Option[UserGroup]] =
    IO.blocking {
      val statement = connection.prepareStatement(findBySlugSql)
      try
        statement.setString(1, slug.value)
        val resultSet = statement.executeQuery()
        try if resultSet.next() then Some(readDetailBase(resultSet)) else None
        finally resultSet.close()
      finally statement.close()
    }.flatMap {
      case Some(group) => listMembers(connection, group.id, listMembersSql).map(members => Some(group.copy(members = members)))
      case None => IO.pure(None)
    }

  def listGroupSlugsForMember(connection: Connection, username: Username): IO[Set[UserGroupSlug]] =
    IO.blocking {
      val statement = connection.prepareStatement(listGroupSlugsForMemberSql)
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

  def insert(connection: Connection, ownerUsername: Username, request: CreateUserGroupRequest): IO[UserGroup] =
    IO.blocking {
      val now = Instant.now()
      val groupId = UserGroupId(UUID.randomUUID())
      val statement = connection.prepareStatement(insertSql)
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
            val ownerMembershipStatement = connection.prepareStatement(insertOwnerMembershipSql)
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
      listMembers(connection, group.id, listMembersSql).map(members => group.copy(members = members))
    }

  def update(connection: Connection, groupId: UserGroupId, request: UpdateUserGroupRequest): IO[Unit] =
    IO.blocking {
      val statement = connection.prepareStatement(updateSql)
      try
        statement.setString(1, request.name.value)
        statement.setString(2, request.description.value)
        statement.setTimestamp(3, Timestamp.from(Instant.now()))
        statement.setObject(4, groupId.value)
        statement.executeUpdate()
        ()
      finally statement.close()
    }

  def delete(connection: Connection, groupId: UserGroupId): IO[Unit] =
    IO.blocking {
      val statement = connection.prepareStatement(deleteSql)
      try
        statement.setObject(1, groupId.value)
        statement.executeUpdate()
        ()
      finally statement.close()
    }

  def userExists(connection: Connection, username: Username): IO[Boolean] =
    IO.blocking {
      val statement = connection.prepareStatement(userExistsSql)
      try
        statement.setString(1, username.value)
        val resultSet = statement.executeQuery()
        try resultSet.next()
        finally resultSet.close()
      finally statement.close()
    }

  def addMember(connection: Connection, groupId: UserGroupId, request: AddUserGroupMemberRequest): IO[AddMemberTableResult] =
    for
      membershipExists <- IO.blocking {
        val statement = connection.prepareStatement(membershipExistsSql)
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
          val statement = connection.prepareStatement(addMemberSql)
          try
            statement.setObject(1, groupId.value)
            statement.setString(2, request.username.value)
            statement.setString(3, AddUserGroupMemberRole.toDatabase(request.role))
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

  def updateMemberRole(
    connection: Connection,
    groupId: UserGroupId,
    targetUsername: Username,
    role: UserGroupRole
  ): IO[UpdateMemberRoleTableResult] =
    IO.blocking {
      val statement = connection.prepareStatement(updateMemberRoleSql)
      try
        statement.setString(1, UserGroupRole.toDatabase(role))
        statement.setObject(2, groupId.value)
        statement.setString(3, targetUsername.value)
        val updatedRows = statement.executeUpdate()
        if updatedRows == 0 then UpdateMemberRoleTableResult.MemberNotFound else UpdateMemberRoleTableResult.Updated
      finally statement.close()
    }

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
              val statement = connection.prepareStatement(updateOwnerUsernameSql)
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

  def removeMember(
    connection: Connection,
    groupId: UserGroupId,
    targetUsername: Username
  ): IO[RemoveMemberTableResult] =
    IO.blocking {
      val statement = connection.prepareStatement(deleteMemberSql)
      try
        statement.setObject(1, groupId.value)
        statement.setString(2, targetUsername.value)
        val deletedRows = statement.executeUpdate()
        if deletedRows == 0 then RemoveMemberTableResult.MemberNotFound else RemoveMemberTableResult.Removed
      finally statement.close()
    }
