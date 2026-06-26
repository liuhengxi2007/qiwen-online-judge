package domains.judge.api

import cats.effect.IO
import cats.syntax.all.*
import domains.auth.api.PublicApi
import domains.hack.api.RecordHackAttemptResult
import domains.hack.objects.HackId
import domains.problem.api.ProblemDataStorageContext
import domains.submission.api.QueueHackRejudgeForProblem
import io.circe.Encoder
import judgeprotocol.objects.request.ReportHackResultRequest
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.{Method, Request, Status}
import shared.api.{ApiPath, HttpApiError, PathParams}
import shared.objects.transport.SuccessResponse

import java.sql.Connection

/** judge worker 完成 hack attempt 的公开 API；记录 hack 结果并在成功时触发题目提交重判。API 对齐例外：worker 通道只供 judger 调用，不提供站点前端 wrapper。 */
final case class CompleteHackAttempt(
  judgeConfig: JudgeConfig,
  problemDataStorage: ProblemDataStorageContext
) extends PublicApi[(HackId, ReportHackResultRequest), SuccessResponse]:

  override val method: Method = Method.POST
  override val path: ApiPath = ApiPath("/api/worker/judge/hacks/:hackId/complete")
  override val successStatus: Status = Status.Ok
  override protected val outputEncoder: Encoder[SuccessResponse] = summon[Encoder[SuccessResponse]]

  /** 校验 worker token，解析 hack id 和上报结果。 */
  override def decode(request: Request[IO], pathParams: PathParams): IO[(HackId, ReportHackResultRequest)] =
    for
      _ <- JudgeTokenAuth.ensureJudgeToken(request, judgeConfig)
      hackId <- HttpApiError.fromEitherBadRequest(pathParams.require("hackId").flatMap(HackId.parse))
      resultRequest <- request.as[ReportHackResultRequest]
    yield hackId -> resultRequest

  /** 写入 hack 完成状态；成功 hack 会物化数据并把相关提交低优先级重判。 */
  override def plan(connection: Connection, input: (HackId, ReportHackResultRequest)): IO[SuccessResponse] =
    val (hackId, request) = input
    for
      maybeProblemId <- RecordHackAttemptResult(problemDataStorage).plan(connection, RecordHackAttemptResult.input(hackId, request))
      _ <- maybeProblemId.traverse_(problemId => QueueHackRejudgeForProblem.plan(connection, problemId))
    yield SuccessResponse(code = None, message = None, params = Map.empty)
