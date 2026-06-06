package domains.hack.api

import cats.effect.IO
import domains.auth.api.InternalOnlyApi
import domains.hack.objects.HackId
import domains.hack.table.hack.HackJudgeTable
import domains.submission.objects.SubmissionLanguage
import domains.submission.objects.internal.ClaimedSubmission
import domains.user.objects.Username
import judgeprotocol.objects.response.JudgeResult
import org.http4s.Method
import shared.api.ApiPath

import java.sql.Connection
import java.time.Instant

final case class ClaimNextHackAttemptInput(
  languages: List[SubmissionLanguage],
  startedAt: Instant
)

final case class ClaimedHackAttempt(
  hackId: HackId,
  targetSubmission: ClaimedSubmission,
  authorUsername: Username,
  subtaskIndex: Int,
  input: String,
  strategyProviderSource: Option[String],
  oldResult: JudgeResult
)

object ClaimNextHackAttempt extends InternalOnlyApi[ClaimNextHackAttemptInput, Option[ClaimedHackAttempt]]:

  override val method: Method = Method.POST
  override val path: ApiPath = ApiPath("/api/internal/hacks/judge/claim-next")

  def input(languages: List[SubmissionLanguage], startedAt: Instant): ClaimNextHackAttemptInput =
    ClaimNextHackAttemptInput(languages = languages, startedAt = startedAt)

  override def plan(connection: Connection, input: ClaimNextHackAttemptInput): IO[Option[ClaimedHackAttempt]] =
    HackJudgeTable.claimNextForLanguages(connection, input.languages, input.startedAt).map {
      _.map(record =>
        ClaimedHackAttempt(
          hackId = record.hackId,
          targetSubmission = record.targetSubmission,
          authorUsername = record.authorUsername,
          subtaskIndex = record.subtaskIndex,
          input = record.input,
          strategyProviderSource = record.strategyProviderSource,
          oldResult = record.oldResult
        )
      )
    }
