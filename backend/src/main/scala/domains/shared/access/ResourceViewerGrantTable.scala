package domains.shared.access

import cats.effect.IO
import domains.auth.model.Username

import java.sql.{Connection, PreparedStatement, ResultSet, Timestamp}
import java.time.Instant

object ResourceViewerGrantTable:

  val initTableSql: String =
    """
      |create table if not exists resource_viewer_grants (
      |  resource_kind varchar(32) not null check (resource_kind in ('problem', 'problem_set')),
      |  resource_id uuid not null,
      |  subject_kind varchar(32) not null check (subject_kind in ('user', 'user_group')),
      |  subject_key varchar(64) not null,
      |  created_at timestamp not null,
      |  primary key (resource_kind, resource_id, subject_kind, subject_key)
      |);
      |create index if not exists idx_resource_viewer_grants_resource
      |  on resource_viewer_grants(resource_kind, resource_id);
      |create index if not exists idx_resource_viewer_grants_subject
      |  on resource_viewer_grants(subject_kind, subject_key);
      |""".stripMargin

  val listForResourceSql: String =
    """
      |select resource_kind, resource_id, subject_kind, subject_key, created_at
      |from resource_viewer_grants
      |where resource_kind = ? and resource_id = ?
      |order by subject_kind asc, subject_key asc
      |""".stripMargin

  val deleteForResourceSql: String =
    """
      |delete from resource_viewer_grants
      |where resource_kind = ? and resource_id = ?
      |""".stripMargin

  val insertGrantSql: String =
    """
      |insert into resource_viewer_grants (resource_kind, resource_id, subject_kind, subject_key, created_at)
      |values (?, ?, ?, ?, ?)
      |on conflict (resource_kind, resource_id, subject_kind, subject_key) do nothing
      |""".stripMargin

  val hasDirectUserGrantSql: String =
    """
      |select 1
      |from resource_viewer_grants
      |where resource_kind = ?
      |  and resource_id = ?
      |  and subject_kind = 'user'
      |  and subject_key = ?
      |limit 1
      |""".stripMargin

  val hasGrantedUserGroupSql: String =
    """
      |select 1
      |from resource_viewer_grants rvg
      |join user_groups ug on ug.slug = rvg.subject_key
      |join user_group_memberships ugm on ugm.user_group_id = ug.id
      |where rvg.resource_kind = ?
      |  and rvg.resource_id = ?
      |  and rvg.subject_kind = 'user_group'
      |  and ugm.username = ?
      |limit 1
      |""".stripMargin

  def initialize(connection: Connection): IO[Unit] =
    IO.blocking {
      val statement = connection.createStatement()
      try statement.execute(initTableSql)
      finally statement.close()
    }

  def listForResource(
    connection: Connection,
    resourceKind: ResourceKind,
    resourceId: ResourceId
  ): IO[List[ResourceViewerGrant]] =
    IO.blocking {
      val statement = connection.prepareStatement(listForResourceSql)
      try
        statement.setString(1, ResourceKind.toDatabase(resourceKind))
        statement.setObject(2, resourceId.value)
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
    grants: List[AccessSubject]
  ): IO[Unit] =
    for
      _ <- deleteForResource(connection, resourceKind, resourceId)
      _ <- insertGrants(connection, resourceKind, resourceId, grants)
    yield ()

  def deleteAllForResource(
    connection: Connection,
    resourceKind: ResourceKind,
    resourceId: ResourceId
  ): IO[Unit] =
    deleteForResource(connection, resourceKind, resourceId)

  def hasDirectUserGrant(
    connection: Connection,
    resourceKind: ResourceKind,
    resourceId: ResourceId,
    username: Username
  ): IO[Boolean] =
    exists(connection, hasDirectUserGrantSql) { statement =>
      statement.setString(1, ResourceKind.toDatabase(resourceKind))
      statement.setObject(2, resourceId.value)
      statement.setString(3, username.value)
    }

  def hasAnyGrantedUserGroup(
    connection: Connection,
    resourceKind: ResourceKind,
    resourceId: ResourceId,
    username: Username
  ): IO[Boolean] =
    exists(connection, hasGrantedUserGroupSql) { statement =>
      statement.setString(1, ResourceKind.toDatabase(resourceKind))
      statement.setObject(2, resourceId.value)
      statement.setString(3, username.value)
    }

  private def deleteForResource(
    connection: Connection,
    resourceKind: ResourceKind,
    resourceId: ResourceId
  ): IO[Unit] =
    IO.blocking {
      val statement = connection.prepareStatement(deleteForResourceSql)
      try
        statement.setString(1, ResourceKind.toDatabase(resourceKind))
        statement.setObject(2, resourceId.value)
        statement.executeUpdate()
        ()
      finally statement.close()
    }

  private def insertGrants(
    connection: Connection,
    resourceKind: ResourceKind,
    resourceId: ResourceId,
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
            statement.setString(3, AccessSubject.subjectKind(subject))
            statement.setString(4, AccessSubject.subjectKey(subject))
            statement.setTimestamp(5, Timestamp.from(now))
            statement.addBatch()
          }
        statement.executeBatch()
        ()
      finally statement.close()
    }

  private def exists(
    connection: Connection,
    sql: String
  )(bind: PreparedStatement => Unit): IO[Boolean] =
    IO.blocking {
      val statement = connection.prepareStatement(sql)
      try
        bind(statement)
        val resultSet = statement.executeQuery()
        try resultSet.next()
        finally resultSet.close()
      finally statement.close()
    }

  private def readGrant(resultSet: ResultSet): ResourceViewerGrant =
    ResourceViewerGrant(
      resourceKind = ResourceKind.fromDatabaseUnsafe(resultSet.getString("resource_kind")),
      resourceId = ResourceId(resultSet.getObject("resource_id", classOf[java.util.UUID])),
      subject = readSubject(resultSet.getString("subject_kind"), resultSet.getString("subject_key")),
      createdAt = resultSet.getTimestamp("created_at").toInstant
    )

  private def readSubject(subjectKind: String, subjectKey: String): AccessSubject =
    subjectKind match
      case "user" => AccessSubject.User(Username.canonical(subjectKey))
      case "user_group" => AccessSubject.UserGroup(domains.usergroup.model.UserGroupSlug.unsafe(subjectKey))
      case other => throw IllegalArgumentException(s"Unknown access subject kind: $other")
