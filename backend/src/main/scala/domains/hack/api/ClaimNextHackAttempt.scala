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

/** 判题 worker 领取 hack attempt 的内部输入；包含支持语言和写入 running 的开始时间。 */
final case class ClaimNextHackAttemptInput(
  languages: List[SubmissionLanguage],
  startedAt: Instant
)

/** 已领取的 hack attempt；携带目标提交任务、hack 输入和原始判题结果，供 worker 构建 HackTask。 */
final case class ClaimedHackAttempt(
  hackId: HackId,
  targetSubmission: ClaimedSubmission,
  authorUsername: Username,
  subtaskIndex: Int,
  input: String,
  strategyProviderSource: Option[String],
  oldResult: JudgeResult
)

/** 内部 hack claim API；judge worker 没有普通提交时会领取可执行的 hack attempt。 */
object ClaimNextHackAttempt extends InternalOnlyApi[ClaimNextHackAttemptInput, Option[ClaimedHackAttempt]]:

  override val method: Method = Method.POST
  override val path: ApiPath = ApiPath("/api/internal/hacks/judge/claim-next")

  /** 构造 hack claim 输入。 */
  def input(languages: List[SubmissionLanguage], startedAt: Instant): ClaimNextHackAttemptInput =
    ClaimNextHackAttemptInput(languages = languages, startedAt = startedAt)

  /** 原子领取一个 queued hack，并转换为 worker 需要的已领取结构。 */
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
