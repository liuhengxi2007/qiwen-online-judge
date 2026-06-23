package domains.submission.table.submission

import cats.effect.IO
import cats.syntax.all.*
import domains.submission.objects.{SubmissionLanguage, SubmissionSourceCode}
import domains.submission.objects.internal.SubmissionProgramManifest
import domains.submission.utils.{SubmissionProgramStorage, SubmissionProgramStorageContext}
import io.circe.syntax.*

import java.sql.Connection
import java.util.UUID

/** 旧提交源码到 program_manifest 的回填器；把 legacy source_code 写入对象存储并记录 manifest。 */
object SubmissionProgramManifestBackfill:

  /** 当 legacy language/source_code 列存在时执行回填；新表结构无旧列时无副作用。 */
  def run(connection: Connection, submissionProgramStorage: SubmissionProgramStorageContext): IO[Unit] =
    for
      hasLegacySourceCode <- columnExists(connection, "source_code")
      hasLegacyLanguage <- columnExists(connection, "language")
      _ <-
        if hasLegacySourceCode && hasLegacyLanguage then
          readLegacySubmissionSources(connection).flatMap { rows =>
            rows.traverse_ { row =>
              val manifest = SubmissionProgramManifest.singleDefault(row.id, row.language, row.sourceCode)
              for
                _ <- SubmissionProgramStorage.writeSource(submissionProgramStorage, manifest.programs(SubmissionProgramManifest.DefaultProgramKey).sourceKey, row.sourceCode)
                _ <- writeProgramManifest(connection, row.id, manifest)
              yield ()
            }
          }
        else IO.unit
    yield ()

  private val columnExistsSql: String =
    """
      |select 1
      |from information_schema.columns
      |where table_schema = 'public'
      |  and table_name = 'submissions'
      |  and column_name = ?
      |""".stripMargin

  private def columnExists(connection: Connection, columnName: String): IO[Boolean] =
    IO.blocking {
      val statement = connection.prepareStatement(columnExistsSql)
      try
        statement.setString(1, columnName)
        val resultSet = statement.executeQuery()
        try resultSet.next()
        finally resultSet.close()
      finally statement.close()
    }

  private val selectLegacySubmissionSourcesSql: String =
    """
      |select id, language, source_code
      |from submissions
      |where program_manifest is null
      |order by submitted_at asc, id asc
      |""".stripMargin

  private def readLegacySubmissionSources(connection: Connection): IO[List[LegacySubmissionSource]] =
    IO.blocking {
      val statement = connection.prepareStatement(selectLegacySubmissionSourcesSql)
      try
        val resultSet = statement.executeQuery()
        try
          Iterator
            .continually(resultSet.next())
            .takeWhile(identity)
            .map { _ =>
              val id = resultSet.getObject("id", classOf[UUID])
              val language =
                SubmissionLanguage
                  .parse(resultSet.getString("language"))
                  .fold(message => throw IllegalStateException(s"Invalid legacy submission language: $message"), identity)
              val sourceCode =
                SubmissionSourceCode
                  .parse(resultSet.getString("source_code"))
                  .fold(message => throw IllegalStateException(s"Invalid legacy submission source: $message"), identity)
              LegacySubmissionSource(id, language, sourceCode)
            }
            .toList
        finally resultSet.close()
      finally statement.close()
    }

  private val updateProgramManifestSql: String =
    """
      |update submissions
      |set program_manifest = ?::jsonb
      |where id = ?
      |  and program_manifest is null
      |""".stripMargin

  private def writeProgramManifest(
    connection: Connection,
    submissionUuid: UUID,
    manifest: SubmissionProgramManifest
  ): IO[Unit] =
    IO.blocking {
      val statement = connection.prepareStatement(updateProgramManifestSql)
      try
        statement.setString(1, manifest.asJson.noSpaces)
        statement.setObject(2, submissionUuid)
        statement.executeUpdate()
        ()
      finally statement.close()
    }

  private final case class LegacySubmissionSource(
    id: UUID,
    language: SubmissionLanguage,
    sourceCode: SubmissionSourceCode
  )
