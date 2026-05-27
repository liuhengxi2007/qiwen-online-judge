package domains.problem.table.problem_data_file



import cats.effect.IO
import cats.syntax.all.*
import domains.problem.objects.{ProblemDataPath, ProblemId, ProblemSlug}
import domains.problem.objects.internal.{ProblemDataManifest, ProblemDataManifestEntry}

import java.sql.Connection
import java.time.Instant

object ProblemDataFileTable:

  def initialize(connection: Connection): IO[Unit] =
    ProblemDataFileTableSchema.initialize(connection)

  def replaceForProblem(
    connection: Connection,
    problemId: ProblemId,
    entries: List[ProblemDataManifestEntry],
    createdAt: Instant
  ): IO[Unit] =
    deleteAllForProblem(connection, problemId) *> entries.traverse_(entry => insert(connection, problemId, entry, createdAt))

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
