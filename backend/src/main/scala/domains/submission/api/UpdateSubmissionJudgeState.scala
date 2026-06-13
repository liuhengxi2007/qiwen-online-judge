package domains.submission.api

import cats.effect.IO
import domains.auth.api.InternalOnlyApi
import domains.submission.objects.SubmissionId
import domains.submission.objects.internal.SubmissionJudgeState
import domains.submission.table.submission.SubmissionJudgeTable
import org.http4s.Method
import shared.api.ApiPath
import shared.objects.response.SuccessResponse

import java.sql.Connection

/** 内部更新提交判题状态的输入；由 worker 领取和完成流程产生。 */
final case class UpdateSubmissionJudgeStateInput(
  submissionId: SubmissionId,
  judgeState: SubmissionJudgeState
)

/** 内部提交判题状态写入 API；只供 judge worker 调度流程使用。 */
object UpdateSubmissionJudgeState extends InternalOnlyApi[UpdateSubmissionJudgeStateInput, SuccessResponse]:

  override val method: Method = Method.POST
  override val path: ApiPath = ApiPath("/api/internal/submissions/judge/state/update")

  /** 构造状态更新输入。 */
  def input(submissionId: SubmissionId, judgeState: SubmissionJudgeState): UpdateSubmissionJudgeStateInput =
    UpdateSubmissionJudgeStateInput(submissionId = submissionId, judgeState = judgeState)

  /** 将给定判题状态写入提交表；调用方负责先完成生命周期合法性校验。 */
  override def plan(connection: Connection, input: UpdateSubmissionJudgeStateInput): IO[SuccessResponse] =
    SubmissionJudgeTable
      .updateJudgeState(connection, input.submissionId, input.judgeState)
      .as(SuccessResponse(code = None, message = None, params = Map.empty))
