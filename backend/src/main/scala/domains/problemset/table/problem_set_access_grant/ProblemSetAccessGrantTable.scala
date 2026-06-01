package domains.problemset.table.problem_set_access_grant

import cats.effect.IO
import domains.problemset.objects.ProblemSetId
import domains.problemset.table.problem_set_access_grant.ProblemSetAccessGrantTableSupport.*
import shared.objects.access.{AccessSubject, GrantRole}

import java.sql.{Connection, ResultSet, Timestamp}
import java.time.Instant

object ProblemSetAccessGrantTable:

  def initialize(connection: Connection): IO[Unit] =
    ProblemSetAccessGrantTableSchema.initialize(connection)

  private val listForProblemSetSQL: String =
    """
      |select subject_kind, subject_key
      |from problem_set_access_grants
      |where problem_set_id = ? and grant_role = ?
      |order by subject_kind asc, subject_key asc
      |""".stripMargin

  def listForProblemSet(
    connection: Connection,
    problemSetId: ProblemSetId,
    grantRole: GrantRole
  ): IO[List[AccessSubject]] =
    IO.blocking {
      val statement = connection.prepareStatement(listForProblemSetSQL)
      try
        statement.setObject(1, problemSetId.value)
        statement.setString(2, encodeGrantRoleColumn(grantRole))
        val resultSet = statement.executeQuery()
        try
          Iterator
            .continually(resultSet.next())
            .takeWhile(identity)
            .map(_ => readSubject(resultSet))
            .toList
        finally resultSet.close()
      finally statement.close()
    }

  def replaceForProblemSet(
    connection: Connection,
    problemSetId: ProblemSetId,
    grantRole: GrantRole,
    grants: List[AccessSubject]
  ): IO[Unit] =
    for
      _ <- deleteForProblemSet(connection, problemSetId, grantRole)
      _ <- insertGrants(connection, problemSetId, grantRole, grants)
    yield ()

  private val deleteAllForProblemSetSQL: String =
    """
      |delete from problem_set_access_grants
      |where problem_set_id = ?
      |""".stripMargin

  def deleteAllForProblemSet(connection: Connection, problemSetId: ProblemSetId): IO[Unit] =
    IO.blocking {
      val statement = connection.prepareStatement(deleteAllForProblemSetSQL)
      try
        statement.setObject(1, problemSetId.value)
        statement.executeUpdate()
        ()
      finally statement.close()
    }

  private val deleteForProblemSetAndRoleSQL: String =
    """
      |delete from problem_set_access_grants
      |where problem_set_id = ? and grant_role = ?
      |""".stripMargin

  private def deleteForProblemSet(
    connection: Connection,
    problemSetId: ProblemSetId,
    grantRole: GrantRole
  ): IO[Unit] =
    IO.blocking {
      val statement = connection.prepareStatement(deleteForProblemSetAndRoleSQL)
      try
        statement.setObject(1, problemSetId.value)
        statement.setString(2, encodeGrantRoleColumn(grantRole))
        statement.executeUpdate()
        ()
      finally statement.close()
    }

  private val insertGrantSQL: String =
    """
      |insert into problem_set_access_grants (problem_set_id, grant_role, subject_kind, subject_key, created_at)
      |values (?, ?, ?, ?, ?)
      |on conflict (problem_set_id, grant_role, subject_kind, subject_key) do nothing
      |""".stripMargin

  private def insertGrants(
    connection: Connection,
    problemSetId: ProblemSetId,
    grantRole: GrantRole,
    grants: List[AccessSubject]
  ): IO[Unit] =
    IO.blocking {
      val now = Instant.now()
      val statement = connection.prepareStatement(insertGrantSQL)
      try
        grants
          .distinctBy(subject => (encodeSubjectKindColumn(subject), encodeSubjectKeyColumn(subject)))
          .foreach { subject =>
            statement.setObject(1, problemSetId.value)
            statement.setString(2, encodeGrantRoleColumn(grantRole))
            statement.setString(3, encodeSubjectKindColumn(subject))
            statement.setString(4, encodeSubjectKeyColumn(subject))
            statement.setTimestamp(5, Timestamp.from(now))
            statement.addBatch()
          }
        statement.executeBatch()
        ()
      finally statement.close()
    }

  private def readSubject(resultSet: ResultSet): AccessSubject =
    decodeSubjectColumns(resultSet.getString("subject_kind"), resultSet.getString("subject_key"))
