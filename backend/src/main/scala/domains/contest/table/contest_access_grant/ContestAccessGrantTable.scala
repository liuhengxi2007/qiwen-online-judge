package domains.contest.table.contest_access_grant

import cats.effect.IO
import database.utils.AccessGrantSql.*
import domains.contest.objects.ContestId
import shared.objects.access.{AccessSubject, GrantRole}

import java.sql.{Connection, ResultSet, Timestamp}
import java.time.Instant

/** 比赛访问授权表访问对象，负责 viewer/manager 授权的读取和替换。 */
object ContestAccessGrantTable:

  /** 初始化比赛访问授权表结构。 */
  def initialize(connection: Connection): IO[Unit] =
    ContestAccessGrantTableSchema.initialize(connection)

  private val listForContestSQL: String =
    """
      |select subject_kind, subject_key
      |from contest_access_grants
      |where contest_id = ? and grant_role = ?
      |order by subject_kind asc, subject_key asc
      |""".stripMargin

  /** 读取指定比赛和角色的授权主体列表，按主体类型和 key 稳定排序。 */
  def listForContest(connection: Connection, contestId: ContestId, grantRole: GrantRole): IO[List[AccessSubject]] =
    IO.blocking {
      val statement = connection.prepareStatement(listForContestSQL)
      try
        statement.setObject(1, contestId.value)
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

  /** 用传入授权主体完整替换指定比赛和角色的授权集合。 */
  def replaceForContest(
    connection: Connection,
    contestId: ContestId,
    grantRole: GrantRole,
    grants: List[AccessSubject]
  ): IO[Unit] =
    for
      _ <- deleteForContest(connection, contestId, grantRole)
      _ <- insertGrants(connection, contestId, grantRole, grants)
    yield ()

  private val deleteForContestAndRoleSQL: String =
    """
      |delete from contest_access_grants
      |where contest_id = ? and grant_role = ?
      |""".stripMargin

  private def deleteForContest(connection: Connection, contestId: ContestId, grantRole: GrantRole): IO[Unit] =
    IO.blocking {
      val statement = connection.prepareStatement(deleteForContestAndRoleSQL)
      try
        statement.setObject(1, contestId.value)
        statement.setString(2, encodeGrantRoleColumn(grantRole))
        statement.executeUpdate()
        ()
      finally statement.close()
    }

  private val insertGrantSQL: String =
    """
      |insert into contest_access_grants (contest_id, grant_role, subject_kind, subject_key, created_at)
      |values (?, ?, ?, ?, ?)
      |on conflict (contest_id, grant_role, subject_kind, subject_key) do nothing
      |""".stripMargin

  private def insertGrants(
    connection: Connection,
    contestId: ContestId,
    grantRole: GrantRole,
    grants: List[AccessSubject]
  ): IO[Unit] =
    IO.blocking {
      val now = Instant.now()
      val statement = connection.prepareStatement(insertGrantSQL)
      try
        grants
          .distinctBy(subject => subjectIdentity(subject))
          .foreach { subject =>
            statement.setObject(1, contestId.value)
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
    decodeSubjectColumns(resultSet.getString("subject_kind"), resultSet.getString("subject_key"), "contest access")
