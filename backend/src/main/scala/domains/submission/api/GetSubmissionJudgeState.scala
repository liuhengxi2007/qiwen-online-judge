package domains.submission.api

import cats.effect.IO
import domains.auth.api.InternalOnlyApi
import domains.submission.objects.SubmissionId
import domains.submission.objects.internal.SubmissionJudgeState
import domains.submission.utils.SubmissionJudgeRules
import domains.submission.table.submission.SubmissionQueryTable
import org.http4s.Method
import shared.api.ApiPath

import java.sql.Connection

object GetSubmissionJudgeState extends InternalOnlyApi[SubmissionId, Option[SubmissionJudgeState]]:

  override val method: Method = Method.POST
  override val path: ApiPath = ApiPath("/api/internal/submissions/judge/state")

  override def plan(connection: Connection, submissionId: SubmissionId): IO[Option[SubmissionJudgeState]] =
    SubmissionQueryTable
      .findById(connection, submissionId)
      .map(_.map(SubmissionJudgeRules.fromSubmissionDetail))
