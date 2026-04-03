package domains.usergroup.table

import cats.effect.IO
import domains.auth.model.{AuthUser, DisplayName, Username}
import domains.shared.model.PageResponse
import domains.usergroup.model.{AddUserGroupMemberRequest, AddUserGroupMemberRole, CreateUserGroupRequest, UpdateUserGroupRequest, UserGroup, UserGroupDescription, UserGroupId, UserGroupMemberRecord, UserGroupName, UserGroupRole, UserGroupSlug, UserGroupSummaryView}

import java.sql.{Connection, ResultSet, SQLException, Timestamp}
import java.time.Instant

object UserGroupTable:

  val initTableSql: String =
    """
      |create table if not exists user_groups (
      |  id uuid primary key,
      |  slug varchar(64) not null unique,
      |  name varchar(120) not null,
      |  description text not null,
      |  visibility varchar(32) not null default 'private' check (visibility in ('private', 'group', 'public')),
      |  owner_username varchar(120) not null references auth_users(username),
      |  created_at timestamp not null,
      |  updated_at timestamp not null
      |);
      |""".stripMargin

  val addVisibilityColumnSql: String =
    """
      |do $$
      |begin
      |  if not exists (
      |    select 1
      |    from information_schema.columns
      |    where table_schema = 'public'
      |      and table_name = 'user_groups'
      |      and column_name = 'visibility'
      |  ) then
      |    alter table user_groups add column visibility varchar(32);
      |  end if;
      |end $$;
      |""".stripMargin

  val backfillVisibilitySql: String =
    """
      |update user_groups
      |set visibility = 'private'
      |where visibility is null or btrim(visibility) = ''
      |""".stripMargin

  val setVisibilityNotNullSql: String =
    """
      |alter table user_groups
      |alter column visibility set not null
      |""".stripMargin

  val setVisibilityDefaultSql: String =
    """
      |alter table user_groups
      |alter column visibility set default 'private'
      |""".stripMargin

  val initMembershipTableSql: String =
    """
      |create table if not exists user_group_memberships (
      |  user_group_id uuid not null references user_groups(id) on delete cascade,
      |  username varchar(120) not null references auth_users(username) on delete cascade,
      |  role varchar(32) not null check (role in ('owner', 'manager', 'member')),
      |  joined_at timestamp not null,
      |  primary key (user_group_id, username)
      |);
      |create index if not exists idx_user_group_memberships_username on user_group_memberships(username);
      |""".stripMargin

  val countVisibleSql: String =
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

  val listVisibleSql: String =
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

  val findBySlugSql: String =
    """
      |select id, slug, name, description, owner_username, created_at, updated_at
      |from user_groups
      |where slug = ?
      |""".stripMargin

  val insertSql: String =
    """
      |insert into user_groups (id, slug, name, description, visibility, owner_username, created_at, updated_at)
      |values (?, ?, ?, ?, ?, ?, ?, ?)
      |returning id, slug, name, description, owner_username, created_at, updated_at
      |""".stripMargin

  val insertOwnerMembershipSql: String =
    """
      |insert into user_group_memberships (user_group_id, username, role, joined_at)
      |values (?, ?, 'owner', ?)
      |""".stripMargin

  val listMembersSql: String =
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

  val updateSql: String =
    """
      |update user_groups
      |set name = ?, description = ?, updated_at = ?
      |where id = ?
      |""".stripMargin

  val deleteSql: String =
    """
      |delete from user_groups
      |where id = ?
      |""".stripMargin

  val userExistsSql: String =
    """
      |select 1
      |from auth_users
      |where username = ?
      |""".stripMargin

  val membershipExistsSql: String =
    """
      |select 1
      |from user_group_memberships
      |where user_group_id = ? and username = ?
      |""".stripMargin

  val addMemberSql: String =
    """
      |insert into user_group_memberships (user_group_id, username, role, joined_at)
      |values (?, ?, ?, ?)
      |""".stripMargin

  val updateMemberRoleSql: String =
    """
      |update user_group_memberships
      |set role = ?
      |where user_group_id = ? and username = ?
      |""".stripMargin

  val deleteMemberSql: String =
    """
      |delete from user_group_memberships
      |where user_group_id = ? and username = ?
      |""".stripMargin

  val updateOwnerUsernameSql: String =
    """
      |update user_groups
      |set owner_username = ?, updated_at = ?
      |where id = ?
      |""".stripMargin

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
    IO.blocking {
      val statement = connection.createStatement()
      try
        statement.execute(initTableSql)
        statement.execute(addVisibilityColumnSql)
        statement.executeUpdate(backfillVisibilitySql)
        statement.execute(setVisibilityDefaultSql)
        statement.execute(setVisibilityNotNullSql)
        statement.execute(initMembershipTableSql)
      finally statement.close()
    }

  def listVisibleTo(connection: Connection, actor: AuthUser, page: Int, pageSize: Int): IO[PageResponse[UserGroupSummaryView]] =
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
      case Some(group) => listMembers(connection, group.id).map(members => Some(group.copy(members = members)))
      case None => IO.pure(None)
    }

  def insert(connection: Connection, ownerUsername: Username, request: CreateUserGroupRequest): IO[UserGroup] =
    IO.blocking {
      val now = Instant.now()
      val groupId = UserGroupId.random()
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
          else throw new IllegalStateException("Insert succeeded but returned no user group")
        finally resultSet.close()
      finally statement.close()
    }.flatMap { group =>
      listMembers(connection, group.id).map(members => group.copy(members = members))
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

  private def listMembers(connection: Connection, groupId: UserGroupId): IO[List[UserGroupMemberRecord]] =
    IO.blocking {
      val statement = connection.prepareStatement(listMembersSql)
      try
        statement.setObject(1, groupId.value)
        val resultSet = statement.executeQuery()
        try Iterator.continually(resultSet.next()).takeWhile(identity).map(_ => readMember(resultSet)).toList
        finally resultSet.close()
      finally statement.close()
    }

  private def readSummary(resultSet: ResultSet): UserGroupSummaryView =
    UserGroupSummaryView(
      id = UserGroupId(resultSet.getObject("id", classOf[java.util.UUID])),
      slug = UserGroupSlug(resultSet.getString("slug")),
      name = UserGroupName(resultSet.getString("name")),
      description = UserGroupDescription(resultSet.getString("description")),
      ownerUsername = Username.canonical(resultSet.getString("owner_username")),
      createdAt = resultSet.getTimestamp("created_at").toInstant,
      updatedAt = resultSet.getTimestamp("updated_at").toInstant
    )

  private def readDetailBase(resultSet: ResultSet): UserGroup =
    UserGroup(
      id = UserGroupId(resultSet.getObject("id", classOf[java.util.UUID])),
      slug = UserGroupSlug(resultSet.getString("slug")),
      name = UserGroupName(resultSet.getString("name")),
      description = UserGroupDescription(resultSet.getString("description")),
      ownerUsername = Username.canonical(resultSet.getString("owner_username")),
      members = Nil,
      createdAt = resultSet.getTimestamp("created_at").toInstant,
      updatedAt = resultSet.getTimestamp("updated_at").toInstant
    )

  private def readMember(resultSet: ResultSet): UserGroupMemberRecord =
    UserGroupMemberRecord(
      username = Username.canonical(resultSet.getString("username")),
      displayName = DisplayName(resultSet.getString("display_name")),
      role = UserGroupRole.fromDatabaseUnsafe(resultSet.getString("role")),
      joinedAt = resultSet.getTimestamp("joined_at").toInstant
    )
