package domains.problem.table.problem_access_grant

import cats.effect.IO
import domains.problem.objects.ProblemId
import domains.problem.table.problem_access_grant.ProblemAccessGrantTableSupport.*
import shared.objects.access.{AccessSubject, GrantRole}

import java.sql.{Connection, ResultSet, Timestamp}
import java.time.Instant

object ProblemAccessGrantTable:

  def initialize(connection: Connection): IO[Unit] =
    ProblemAccessGrantTableSchema.initialize(connection)

  private val listForProblemSQL: String =
    """
      |select subject_kind, subject_key
      |from problem_access_grants
      |where problem_id = ? and grant_role = ?
      |order by subject_kind asc, subject_key asc
      |""".stripMargin

  def listForProblem(
    connection: Connection,
    problemId: ProblemId,
    grantRole: GrantRole
  ): IO[List[AccessSubject]] =
    IO.blocking {
      val statement = connection.prepareStatement(listForProblemSQL)
      try
        statement.setObject(1, problemId.value)
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

  def replaceForProblem(
    connection: Connection,
    problemId: ProblemId,
    grantRole: GrantRole,
    grants: List[AccessSubject]
  ): IO[Unit] =
    for
      _ <- deleteForProblem(connection, problemId, grantRole)
      _ <- insertGrants(connection, problemId, grantRole, grants)
    yield ()

  private val deleteAllForProblemSQL: String =
    """
      |delete from problem_access_grants
      |where problem_id = ?
      |""".stripMargin

  def deleteAllForProblem(connection: Connection, problemId: ProblemId): IO[Unit] =
    IO.blocking {
      val statement = connection.prepareStatement(deleteAllForProblemSQL)
      try
        statement.setObject(1, problemId.value)
        statement.executeUpdate()
        ()
      finally statement.close()
    }

  private val deleteForProblemAndRoleSQL: String =
    """
      |delete from problem_access_grants
      |where problem_id = ? and grant_role = ?
      |""".stripMargin

  private def deleteForProblem(
    connection: Connection,
    problemId: ProblemId,
    grantRole: GrantRole
  ): IO[Unit] =
    IO.blocking {
      val statement = connection.prepareStatement(deleteForProblemAndRoleSQL)
      try
        statement.setObject(1, problemId.value)
        statement.setString(2, encodeGrantRoleColumn(grantRole))
        statement.executeUpdate()
        ()
      finally statement.close()
    }

  private val insertGrantSQL: String =
    """
      |insert into problem_access_grants (problem_id, grant_role, subject_kind, subject_key, created_at)
      |values (?, ?, ?, ?, ?)
      |on conflict (problem_id, grant_role, subject_kind, subject_key) do nothing
      |""".stripMargin

  private def insertGrants(
    connection: Connection,
    problemId: ProblemId,
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
            statement.setObject(1, problemId.value)
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
