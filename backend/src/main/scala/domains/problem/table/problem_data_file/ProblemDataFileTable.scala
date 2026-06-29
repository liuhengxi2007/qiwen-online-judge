package domains.problem.table.problem_data_file



import cats.effect.IO
import cats.syntax.all.*
import domains.problem.objects.{ProblemDataPath, ProblemId, ProblemSlug}
import domains.problem.objects.internal.{ProblemDataManifest, ProblemDataManifestEntry}

import java.sql.Connection
import java.time.Instant

/** 题目数据文件清单表入口；记录对象存储中每个文件的路径、大小和 sha256。 */
object ProblemDataFileTable:

  /** 初始化题目数据文件清单表。 */
  def initialize(connection: Connection): IO[Unit] =
    ProblemDataFileTableSchema.initialize(connection)

  /** 用给定清单完整替换题目的文件记录；适用于全量导入或重建。 */
  def replaceForProblem(
    connection: Connection,
    problemId: ProblemId,
    entries: List[ProblemDataManifestEntry],
    createdAt: Instant
  ): IO[Unit] =
    deleteAllForProblem(connection, problemId) *> entries.traverse_(entry => insert(connection, problemId, entry, createdAt))

  /** 增量写入或更新题目文件清单记录；适用于单文件上传和 hack 物化。 */
  def upsertForProblem(
    connection: Connection,
    problemId: ProblemId,
    entries: List[ProblemDataManifestEntry],
    createdAt: Instant
  ): IO[Unit] =
    entries.traverse_(entry => upsert(connection, problemId, entry, createdAt))

  private val listSQL: String =
    """
      |select relative_path, size_bytes, sha256
      |from problem_data_files
      |where problem_id = ?
      |order by relative_path asc
      |""".stripMargin

  /** 按路径顺序列出题目文件清单条目。 */
  def listForProblem(connection: Connection, problemId: ProblemId): IO[List[ProblemDataManifestEntry]] =
    IO.blocking {
      val statement = connection.prepareStatement(listSQL)
      try
        statement.setObject(1, problemId.value)
        val resultSet = statement.executeQuery()
        try
          Iterator
            .continually(resultSet.next())
            .takeWhile(identity)
            .map(_ => readEntry(resultSet))
            .toList
        finally resultSet.close()
      finally statement.close()
    }

  private val deleteByPathSQL: String =
    """
      |delete from problem_data_files
      |where problem_id = ? and relative_path = ?
      |""".stripMargin

  /** 删除题目清单中的单个路径记录。 */
  def deleteForProblemPath(connection: Connection, problemId: ProblemId, path: ProblemDataPath): IO[Unit] =
    IO.blocking {
      val statement = connection.prepareStatement(deleteByPathSQL)
      try
        statement.setObject(1, problemId.value)
        statement.setString(2, path.value)
        statement.executeUpdate()
        ()
      finally statement.close()
    }

  private val deleteAllSQL: String =
    """
      |delete from problem_data_files
      |where problem_id = ?
      |""".stripMargin

  /** 删除题目的所有数据清单记录。 */
  def deleteAllForProblem(connection: Connection, problemId: ProblemId): IO[Unit] =
    IO.blocking {
      val statement = connection.prepareStatement(deleteAllSQL)
      try
        statement.setObject(1, problemId.value)
        statement.executeUpdate()
        ()
      finally statement.close()
    }

  private def deleteExceptPathsSQL(pathCount: Int): String =
    val placeholders = List.fill(pathCount)("?").mkString(", ")
    s"""
      |delete from problem_data_files
      |where problem_id = ?
      |  and relative_path not in ($placeholders)
      |""".stripMargin

  /** 删除不在保留集合中的清单记录；空集合会清空题目所有清单。 */
  def deleteExceptPaths(connection: Connection, problemId: ProblemId, paths: Set[ProblemDataPath]): IO[Unit] =
    if paths.isEmpty then deleteAllForProblem(connection, problemId)
    else
      IO.blocking {
        val statement = connection.prepareStatement(deleteExceptPathsSQL(paths.size))
        try
          statement.setObject(1, problemId.value)
          paths.toList.sortBy(_.value).zipWithIndex.foreach { case (path, index) =>
            statement.setString(index + 2, path.value)
          }
          statement.executeUpdate()
          ()
        finally statement.close()
      }

  /** 读取题目清单记录并构造判题使用的数据 manifest。 */
  def manifestForProblem(connection: Connection, problemId: ProblemId, problemSlug: ProblemSlug): IO[ProblemDataManifest] =
    listForProblem(connection, problemId).map(entries => ProblemDataManifest.fromEntries(problemSlug, entries))

  private val insertSQL: String =
    """
      |insert into problem_data_files (
      |  problem_id,
      |  relative_path,
      |  size_bytes,
      |  sha256,
      |  created_at
      |) values (?, ?, ?, ?, ?)
      |""".stripMargin

  private def insert(
    connection: Connection,
    problemId: ProblemId,
    entry: ProblemDataManifestEntry,
    createdAt: Instant
  ): IO[Unit] =
    IO.blocking {
      val statement = connection.prepareStatement(insertSQL)
      try
        statement.setObject(1, problemId.value)
        statement.setString(2, entry.path.value)
        statement.setLong(3, entry.sizeBytes)
        statement.setString(4, entry.sha256)
        statement.setTimestamp(5, java.sql.Timestamp.from(createdAt))
        statement.executeUpdate()
        ()
      finally statement.close()
    }

  private val upsertSQL: String =
    """
      |insert into problem_data_files (
      |  problem_id,
      |  relative_path,
      |  size_bytes,
      |  sha256,
      |  created_at
      |) values (?, ?, ?, ?, ?)
      |on conflict (problem_id, relative_path)
      |do update set
      |  size_bytes = excluded.size_bytes,
      |  sha256 = excluded.sha256,
      |  created_at = excluded.created_at
      |""".stripMargin

  private def upsert(
    connection: Connection,
    problemId: ProblemId,
    entry: ProblemDataManifestEntry,
    createdAt: Instant
  ): IO[Unit] =
    IO.blocking {
      val statement = connection.prepareStatement(upsertSQL)
      try
        statement.setObject(1, problemId.value)
        statement.setString(2, entry.path.value)
        statement.setLong(3, entry.sizeBytes)
        statement.setString(4, entry.sha256)
        statement.setTimestamp(5, java.sql.Timestamp.from(createdAt))
        statement.executeUpdate()
        ()
      finally statement.close()
    }

  private def readEntry(resultSet: java.sql.ResultSet): ProblemDataManifestEntry =
    ProblemDataManifestEntry(
      path = ProblemDataPath(resultSet.getString("relative_path")),
      sizeBytes = resultSet.getLong("size_bytes"),
      sha256 = resultSet.getString("sha256")
    )
