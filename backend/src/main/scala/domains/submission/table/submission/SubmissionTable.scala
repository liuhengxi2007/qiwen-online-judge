package domains.submission.table.submission

import cats.effect.IO
import domains.submission.utils.SubmissionProgramStorageContext

import java.sql.Connection

/** 提交表初始化入口；负责 program_manifest 迁移前后两阶段表结构初始化。 */
object SubmissionTable:

  /** 初始化 submissions 表并执行旧 source_code 到对象存储 manifest 的回填。 */
  def initialize(connection: Connection, submissionProgramStorage: SubmissionProgramStorageContext): IO[Unit] =
    for
      _ <- SubmissionTableSchema.initializeBeforeProgramManifestBackfill(connection)
      _ <- SubmissionProgramManifestBackfill.run(connection, submissionProgramStorage)
      _ <- SubmissionTableSchema.initializeAfterProgramManifestBackfill(connection)
    yield ()
