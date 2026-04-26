package database

import cats.effect.IO
import domains.auth.model.Username
import domains.shared.access.{AccessSubject, GrantRole, ResourceAccessGrant, ResourceId, ResourceKind}
import domains.usergroup.model.UserGroupSlug

import java.sql.{Connection, PreparedStatement, ResultSet, Timestamp}
import java.time.Instant

object ResourceAccessGrantTable:

  val initTableSql: String =
    """
      |create table if not exists resource_access_grants (
      |  resource_kind varchar(32) not null check (resource_kind in ('problem', 'problem_set')),
      |  resource_id uuid not null,
      |  grant_role varchar(32) not null check (grant_role in ('viewer', 'manager')),
      |  subject_kind varchar(32) not null check (subject_kind in ('user', 'user_group')),
      |  subject_key varchar(64) not null,
      |  created_at timestamp not null,
      |  primary key (resource_kind, resource_id, grant_role, subject_kind, subject_key)
      |);
      |create index if not exists idx_resource_access_grants_resource
      |  on resource_access_grants(resource_kind, resource_id, grant_role);
      |create index if not exists idx_resource_access_grants_subject
      |  on resource_access_grants(grant_role, subject_kind, subject_key);
      |""".stripMargin

  val backfillLegacyViewerGrantSql: String =
    """
      |insert into resource_access_grants (resource_kind, resource_id, grant_role, subject_kind, subject_key, created_at)
      |select resource_kind, resource_id, 'viewer', subject_kind, subject_key, created_at
      |from resource_viewer_grants
      |on conflict (resource_kind, resource_id, grant_role, subject_kind, subject_key) do nothing
      |""".stripMargin

  val listForResourceSql: String =
    """
      |select resource_kind, resource_id, grant_role, subject_kind, subject_key, created_at
      |from resource_access_grants
      |where resource_kind = ? and resource_id = ? and grant_role = ?
      |order by subject_kind asc, subject_key asc
      |""".stripMargin

  val deleteForResourceAndRoleSql: String =
    """
      |delete from resource_access_grants
      |where resource_kind = ? and resource_id = ? and grant_role = ?
      |""".stripMargin

  val deleteAllForResourceSql: String =
    """
      |delete from resource_access_grants
      |where resource_kind = ? and resource_id = ?
      |""".stripMargin

  val insertGrantSql: String =
    """
      |insert into resource_access_grants (resource_kind, resource_id, grant_role, subject_kind, subject_key, created_at)
      |values (?, ?, ?, ?, ?, ?)
      |on conflict (resource_kind, resource_id, grant_role, subject_kind, subject_key) do nothing
      |""".stripMargin

  val hasDirectUserGrantSql: String =
    """
      |select 1
      |from resource_access_grants
      |where resource_kind = ?
      |  and resource_id = ?
      |  and grant_role = ?
      |  and subject_kind = 'user'
      |  and subject_key = ?
      |limit 1
      |""".stripMargin

  val hasGrantedUserGroupSql: String =
    """
      |select 1
      |from resource_access_grants rag
      |join user_groups ug on ug.slug = rag.subject_key
      |join user_group_memberships ugm on ugm.user_group_id = ug.id
      |where rag.resource_kind = ?
      |  and rag.resource_id = ?
      |  and rag.grant_role = ?
      |  and rag.subject_kind = 'user_group'
      |  and ugm.username = ?
      |limit 1
      |""".stripMargin

  def initialize(connection: Connection): IO[Unit] =
    IO.blocking {
      val statement = connection.createStatement()
      try
        statement.execute(initTableSql)
        val hasLegacyTable =
          Option(connection.getMetaData.getTables(null, null, "resource_viewer_grants", null)).exists { resultSet =>
            try resultSet.next()
            finally resultSet.close()
          }
        if hasLegacyTable then statement.executeUpdate(backfillLegacyViewerGrantSql)
      finally statement.close()
    }

  def listForResource(
    connection: Connection,
    resourceKind: ResourceKind,
    resourceId: ResourceId,
    grantRole: GrantRole
  ): IO[List[ResourceAccessGrant]] =
    IO.blocking {
      val statement = connection.prepareStatement(listForResourceSql)
      try
        statement.setString(1, ResourceKind.toDatabase(resourceKind))
        statement.setObject(2, resourceId.value)
        statement.setString(3, GrantRole.toDatabase(grantRole))
        val resultSet = statement.executeQuery()
        try
          Iterator
            .continually(resultSet.next())
            .takeWhile(identity)
            .map(_ => readGrant(resultSet))
            .toList
        finally resultSet.close()
      finally statement.close()
    }

  def replaceForResource(
    connection: Connection,
    resourceKind: ResourceKind,
    resourceId: ResourceId,
    grantRole: GrantRole,
    grants: List[AccessSubject]
  ): IO[Unit] =
    for
      _ <- deleteForResource(connection, resourceKind, resourceId, grantRole)
      _ <- insertGrants(connection, resourceKind, resourceId, grantRole, grants)
    yield ()

  def deleteAllForResource(
    connection: Connection,
    resourceKind: ResourceKind,
    resourceId: ResourceId
  ): IO[Unit] =
    IO.blocking {
      val statement = connection.prepareStatement(deleteAllForResourceSql)
      try
        statement.setString(1, ResourceKind.toDatabase(resourceKind))
        statement.setObject(2, resourceId.value)
        statement.executeUpdate()
        ()
      finally statement.close()
    }

  def hasDirectUserGrant(
    connection: Connection,
    resourceKind: ResourceKind,
    resourceId: ResourceId,
    grantRole: GrantRole,
    username: Username
  ): IO[Boolean] =
    exists(connection, hasDirectUserGrantSql) { statement =>
      statement.setString(1, ResourceKind.toDatabase(resourceKind))
      statement.setObject(2, resourceId.value)
      statement.setString(3, GrantRole.toDatabase(grantRole))
      statement.setString(4, username.value)
    }

  def hasAnyGrantedUserGroup(
    connection: Connection,
    resourceKind: ResourceKind,
    resourceId: ResourceId,
    grantRole: GrantRole,
    username: Username
  ): IO[Boolean] =
    exists(connection, hasGrantedUserGroupSql) { statement =>
      statement.setString(1, ResourceKind.toDatabase(resourceKind))
      statement.setObject(2, resourceId.value)
      statement.setString(3, GrantRole.toDatabase(grantRole))
      statement.setString(4, username.value)
    }

  private def deleteForResource(
    connection: Connection,
    resourceKind: ResourceKind,
    resourceId: ResourceId,
    grantRole: GrantRole
  ): IO[Unit] =
    IO.blocking {
      val statement = connection.prepareStatement(deleteForResourceAndRoleSql)
      try
        statement.setString(1, ResourceKind.toDatabase(resourceKind))
        statement.setObject(2, resourceId.value)
        statement.setString(3, GrantRole.toDatabase(grantRole))
        statement.executeUpdate()
        ()
      finally statement.close()
    }

  private def insertGrants(
    connection: Connection,
    resourceKind: ResourceKind,
    resourceId: ResourceId,
    grantRole: GrantRole,
    grants: List[AccessSubject]
  ): IO[Unit] =
    IO.blocking {
      val now = Instant.now()
      val statement = connection.prepareStatement(insertGrantSql)
      try
        grants
          .distinctBy(subject => (AccessSubject.subjectKind(subject), AccessSubject.subjectKey(subject)))
          .foreach { subject =>
            statement.setString(1, ResourceKind.toDatabase(resourceKind))
            statement.setObject(2, resourceId.value)
            statement.setString(3, GrantRole.toDatabase(grantRole))
            statement.setString(4, AccessSubject.subjectKind(subject))
            statement.setString(5, AccessSubject.subjectKey(subject))
            statement.setTimestamp(6, Timestamp.from(now))
            statement.addBatch()
          }
        statement.executeBatch()
        ()
      finally statement.close()
    }

  private def exists(connection: Connection, sql: String)(bind: PreparedStatement => Unit): IO[Boolean] =
    IO.blocking {
      val statement = connection.prepareStatement(sql)
      try
        bind(statement)
        val resultSet = statement.executeQuery()
        try resultSet.next()
        finally resultSet.close()
      finally statement.close()
    }

  private def readGrant(resultSet: ResultSet): ResourceAccessGrant =
    ResourceAccessGrant(
      resourceKind =
        ResourceKind
          .fromDatabase(resultSet.getString("resource_kind"))
          .getOrElse(throw IllegalStateException(s"Invalid resource_kind: ${resultSet.getString("resource_kind")}")),
      resourceId = ResourceId(resultSet.getObject("resource_id", classOf[java.util.UUID])),
      grantRole =
        GrantRole
          .fromDatabase(resultSet.getString("grant_role"))
          .getOrElse(throw IllegalStateException(s"Invalid grant_role: ${resultSet.getString("grant_role")}")),
      subject = parseAccessSubject(resultSet.getString("subject_kind"), resultSet.getString("subject_key")),
      createdAt = resultSet.getTimestamp("created_at").toInstant
    )

  private def parseAccessSubject(subjectKind: String, subjectKey: String): AccessSubject =
    subjectKind match
      case "user" => AccessSubject.User(Username.canonical(subjectKey))
      case "user_group" =>
        UserGroupSlug
          .parse(subjectKey)
          .fold(
            message => throw IllegalStateException(s"Invalid access subject slug: $message"),
            AccessSubject.UserGroup(_)
          )
      case other =>
        throw IllegalStateException(s"Invalid access subject kind: $other")
