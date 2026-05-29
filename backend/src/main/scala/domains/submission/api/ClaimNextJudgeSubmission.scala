package domains.submission.api

import cats.effect.IO
import domains.auth.api.InternalOnlyApi
import domains.submission.objects.SubmissionLanguage
import domains.submission.objects.internal.{ClaimedSubmission, SubmissionJudgeState}
import domains.submission.table.submission.SubmissionJudgeTable
import org.http4s.Method
import shared.api.ApiPath

import java.sql.Connection

final case class ClaimNextJudgeSubmissionInput(
  languages: List[SubmissionLanguage],
  runningState: SubmissionJudgeState
)

object ClaimNextJudgeSubmission extends InternalOnlyApi[ClaimNextJudgeSubmissionInput, Option[ClaimedSubmission]]:

  override val method: Method = Method.POST
  override val path: ApiPath = ApiPath("/api/internal/submissions/claim-next-judge")

  def input(languages: List[SubmissionLanguage], runningState: SubmissionJudgeState): ClaimNextJudgeSubmissionInput =
    ClaimNextJudgeSubmissionInput(languages = languages, runningState = runningState)

  override def plan(connection: Connection, input: ClaimNextJudgeSubmissionInput): IO[Option[ClaimedSubmission]] =
    SubmissionJudgeTable.claimNextForLanguages(connection, input.languages, input.runningState)
