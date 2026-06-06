package domains.submission.api

import cats.effect.IO
import domains.auth.api.InternalOnlyApi
import domains.submission.objects.SubmissionId
import domains.submission.table.submission.SubmissionJudgeTable
import org.http4s.Method
import shared.api.ApiPath

import java.sql.Connection

object RequeueStaleHackRevisionSubmission extends InternalOnlyApi[SubmissionId, Boolean]:

  override val method: Method = Method.POST
  override val path: ApiPath = ApiPath("/api/internal/submissions/judge/requeue-stale-hack-revision")

  override def plan(connection: Connection, submissionId: SubmissionId): IO[Boolean] =
    SubmissionJudgeTable.requeueIfHackRevisionStale(connection, submissionId)
