package domains.problem.table.problem

import cats.effect.IO
import domains.problem.objects.{ProblemDataFilename, ProblemId}
import domains.submission.objects.SubmissionResultDisplayMode

import java.sql.{Connection, Timestamp}
import java.time.Instant

/** problems 表中题目数据状态字段的写入入口；由数据上传、删除和 ready 切换流程调用。 */
object ProblemDataStateTable:

  /** 更新题目数据摘要文件名并下线 ready 状态；用于旧版按文件名上传兼容。 */
  def updateData(connection: Connection, problemId: ProblemId, updatedAt: Instant, filename: ProblemDataFilename): IO[Unit] =
    updateData(connection, problemId, updatedAt, Some(filename))

  private val updateDataSQL: String =
    """
      |update problems
      |set data_name = ?, ready = false, result_display_mode = 'score', updated_at = ?
      |where id = ?
      |""".stripMargin

  /** 更新题目数据摘要文件名并把 ready 置为 false；用于文件集合发生变化后的默认状态。 */
  def updateData(
    connection: Connection,
    problemId: ProblemId,
    updatedAt: Instant,
    filename: Option[ProblemDataFilename]
  ): IO[Unit] =
    IO.blocking {
      val statement = connection.prepareStatement(updateDataSQL)
      try
        filename match
          case Some(value) => statement.setString(1, value.value)
          case None => statement.setNull(1, java.sql.Types.VARCHAR)
        statement.setTimestamp(2, Timestamp.from(updatedAt))
        statement.setObject(3, problemId.value)
        statement.executeUpdate()
        ()
      finally statement.close()
    }

  private val updateDataReadySQL: String =
    """
      |update problems
      |set data_name = ?, ready = ?, result_display_mode = ?, updated_at = ?
      |where id = ?
      |""".stripMargin

  /** 更新题目 ready 状态、摘要文件名和结果展示模式；ready=true 只应在判题配置校验后调用。 */
  def updateDataReady(
    connection: Connection,
    problemId: ProblemId,
    updatedAt: Instant,
    filename: Option[ProblemDataFilename],
    ready: Boolean,
    resultDisplayMode: SubmissionResultDisplayMode = SubmissionResultDisplayMode.Score
  ): IO[Unit] =
    IO.blocking {
      val statement = connection.prepareStatement(updateDataReadySQL)
      try
        filename match
          case Some(value) => statement.setString(1, value.value)
          case None => statement.setNull(1, java.sql.Types.VARCHAR)
        statement.setBoolean(2, ready)
        statement.setString(3, SubmissionResultDisplayMode.encode(resultDisplayMode))
        statement.setTimestamp(4, Timestamp.from(updatedAt))
        statement.setObject(5, problemId.value)
        statement.executeUpdate()
        ()
      finally statement.close()
    }
