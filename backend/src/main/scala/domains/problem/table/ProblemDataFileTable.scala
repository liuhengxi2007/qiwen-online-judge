package domains.problem.table

import cats.effect.IO
import cats.syntax.all.*
import domains.problem.application.{ProblemDataManifest, ProblemDataManifestEntry}
import domains.problem.model.{ProblemDataPath, ProblemId, ProblemSlug}

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

  def listForProblem(connection: Connection, problemId: ProblemId): IO[List[ProblemDataManifestEntry]] =
    IO.blocking {
      val statement = connection.prepareStatement(ProblemDataFileTableSql.listSql)
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

  def deleteForProblemPath(connection: Connection, problemId: ProblemId, path: ProblemDataPath): IO[Unit] =
    IO.blocking {
      val statement = connection.prepareStatement(ProblemDataFileTableSql.deleteByPathSql)
      try
        statement.setObject(1, problemId.value)
        statement.setString(2, path.value)
        statement.executeUpdate()
        ()
      finally statement.close()
    }

  def deleteAllForProblem(connection: Connection, problemId: ProblemId): IO[Unit] =
    IO.blocking {
      val statement = connection.prepareStatement(ProblemDataFileTableSql.deleteAllSql)
      try
        statement.setObject(1, problemId.value)
        statement.executeUpdate()
        ()
      finally statement.close()
    }

  def deleteExceptPaths(connection: Connection, problemId: ProblemId, paths: Set[ProblemDataPath]): IO[Unit] =
    if paths.isEmpty then deleteAllForProblem(connection, problemId)
    else
      IO.blocking {
        val statement = connection.prepareStatement(ProblemDataFileTableSql.deleteExceptPathsSql(paths.size))
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

  private def insert(
    connection: Connection,
    problemId: ProblemId,
    entry: ProblemDataManifestEntry,
    createdAt: Instant
  ): IO[Unit] =
    IO.blocking {
      val statement = connection.prepareStatement(ProblemDataFileTableSql.insertSql)
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

  private def upsert(
    connection: Connection,
    problemId: ProblemId,
    entry: ProblemDataManifestEntry,
    createdAt: Instant
  ): IO[Unit] =
    IO.blocking {
      val statement = connection.prepareStatement(ProblemDataFileTableSql.upsertSql)
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
