package domains.submission.api

import cats.effect.IO
import domains.auth.api.InternalOnlyApi
import domains.submission.objects.SubmissionId
import domains.submission.table.submission.SubmissionJudgeTable
import org.http4s.Method
import shared.api.ApiPath

import java.sql.Connection

/** 内部按 hack revision 检查单个提交是否需要重判的 API；judge 完成后调用。 */
object RequeueStaleHackRevisionSubmission extends InternalOnlyApi[SubmissionId, Boolean]:

  override val method: Method = Method.POST
  override val path: ApiPath = ApiPath("/api/internal/submissions/judge/requeue-stale-hack-revision")

  /** 若提交记录的 hack revision 落后于题目则低优先级重新入队，返回是否发生更新。 */
  override def plan(connection: Connection, submissionId: SubmissionId): IO[Boolean] =
    SubmissionJudgeTable.requeueIfHackRevisionStale(connection, submissionId)
