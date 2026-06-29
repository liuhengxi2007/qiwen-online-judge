package domains.problem.api

import cats.effect.IO
import domains.problem.objects.ProblemId
import domains.problem.table.problem.ProblemQueryTable
import domains.submission.objects.SubmissionResultDisplayMode

import java.sql.Connection

/** 提供提交域读取题目结果展示模式的小型查询入口；不做权限判断，仅供内部流程使用。API 对齐例外：提交创建只在 plan 级协作中调用它，不是前端端点。 */
object GetProblemSubmissionResultDisplayMode:

  /** 按题目 id 读取结果展示模式；题目不存在时返回 None。 */
  def plan(connection: Connection, problemId: ProblemId): IO[Option[SubmissionResultDisplayMode]] =
    ProblemQueryTable.findResultDisplayModeById(connection, problemId)
