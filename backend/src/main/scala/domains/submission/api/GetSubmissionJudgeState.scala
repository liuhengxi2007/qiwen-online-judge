package domains.submission.api

import cats.effect.IO
import domains.auth.api.InternalOnlyApi
import domains.submission.objects.SubmissionId
import domains.submission.objects.internal.SubmissionJudgeState
import domains.submission.table.submission.SubmissionQueryTable
import org.http4s.Method
import shared.api.ApiPath

import java.sql.Connection

/** 内部判题状态读取 API；judge worker 完成任务前用它获取当前提交生命周期状态。 */
object GetSubmissionJudgeState extends InternalOnlyApi[SubmissionId, Option[SubmissionJudgeState]]:

  override val method: Method = Method.POST
  override val path: ApiPath = ApiPath("/api/internal/submissions/judge/state")

  /** 按 public id 读取提交判题状态；提交不存在时返回 None。 */
  override def plan(connection: Connection, submissionId: SubmissionId): IO[Option[SubmissionJudgeState]] =
    SubmissionQueryTable
      .findById(connection, submissionId)
      .map(_.map(SubmissionJudgeRules.fromSubmissionRecord))
