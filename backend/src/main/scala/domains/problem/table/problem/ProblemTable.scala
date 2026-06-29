package domains.problem.table.problem

import cats.effect.IO
import domains.problem.table.problem_access_grant.ProblemAccessGrantTable

import java.sql.Connection

/** 题目域表初始化入口；初始化题目主表和题目专用授权表。 */
object ProblemTable:

  /** 创建或迁移题目相关表结构。 */
  def initialize(connection: Connection): IO[Unit] =
    for
      _ <- ProblemTableSchema.initialize(connection)
      _ <- ProblemAccessGrantTable.initialize(connection)
    yield ()
