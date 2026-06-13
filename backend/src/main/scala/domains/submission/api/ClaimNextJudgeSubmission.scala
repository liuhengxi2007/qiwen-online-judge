package domains.submission.api

import cats.effect.IO
import domains.auth.api.InternalOnlyApi
import domains.submission.objects.SubmissionLanguage
import domains.submission.objects.internal.{ClaimedSubmission, SubmissionJudgeState}
import domains.submission.table.submission.SubmissionJudgeTable
import org.http4s.Method
import shared.api.ApiPath

import java.sql.Connection

/** 判题 worker 领取提交任务的内部输入；包含支持语言、将写入的运行态和最低优先级。 */
final case class ClaimNextJudgeSubmissionInput(
  languages: List[SubmissionLanguage],
  runningState: SubmissionJudgeState,
  minPriority: Int
)

/** 内部提交判题 claim API；由 judge worker 领取下一个可判提交。 */
object ClaimNextJudgeSubmission extends InternalOnlyApi[ClaimNextJudgeSubmissionInput, Option[ClaimedSubmission]]:

  override val method: Method = Method.POST
  override val path: ApiPath = ApiPath("/api/internal/submissions/judge/claim-next")

  /** 构造 claim 输入；不做数据库访问。 */
  def input(languages: List[SubmissionLanguage], runningState: SubmissionJudgeState, minPriority: Int): ClaimNextJudgeSubmissionInput =
    ClaimNextJudgeSubmissionInput(languages = languages, runningState = runningState, minPriority = minPriority)

  /** 原子领取一个匹配语言和优先级的提交，并把状态更新为 runningState。 */
  override def plan(connection: Connection, input: ClaimNextJudgeSubmissionInput): IO[Option[ClaimedSubmission]] =
    SubmissionJudgeTable.claimNextForLanguages(connection, input.languages, input.runningState, input.minPriority)
