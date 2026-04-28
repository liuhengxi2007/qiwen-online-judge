package domains.problem.table

object ProblemDataFileTableSql:
  val insertSql: String =
    """
      |insert into problem_data_files (
      |  problem_id,
      |  relative_path,
      |  size_bytes,
      |  sha256,
      |  created_at
      |) values (?, ?, ?, ?, ?)
      |""".stripMargin

  val listSql: String =
    """
      |select relative_path, size_bytes, sha256
      |from problem_data_files
      |where problem_id = ?
      |order by relative_path asc
      |""".stripMargin

  val deleteByPathSql: String =
    """
      |delete from problem_data_files
      |where problem_id = ? and relative_path = ?
      |""".stripMargin

  val deleteAllSql: String =
    """
      |delete from problem_data_files
      |where problem_id = ?
      |""".stripMargin
