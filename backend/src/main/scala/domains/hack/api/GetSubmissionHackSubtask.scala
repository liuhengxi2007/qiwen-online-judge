package domains.hack.api

import cats.effect.IO
import domains.auth.api.AuthenticatedApi
import domains.auth.objects.internal.AuthenticatedUser
import domains.hack.objects.response.HackSubtaskInfo
import domains.problem.utils.ProblemDataStorageContext
import domains.submission.objects.SubmissionId
import domains.submission.utils.SubmissionProgramStorageContext
import io.circe.Encoder
import org.http4s.{Method, Request, Status}
import shared.api.{ApiPath, HttpApiError, PathParams}

import java.sql.Connection

/** 查询提交单个子任务 hack 详情的认证 API；用于创建 hack 前展示旧分数和策略要求。 */
final case class GetSubmissionHackSubtask(
  submissionProgramStorage: SubmissionProgramStorageContext,
  problemDataStorage: ProblemDataStorageContext
) extends AuthenticatedApi[(SubmissionId, Int), HackSubtaskInfo]:

  override val method: Method = Method.GET
  override val path: ApiPath = ApiPath("/api/submissions/:submissionId/hack/subtasks/:subtaskIndex")
  override val successStatus: Status = Status.Ok
  override protected val outputEncoder: Encoder[HackSubtaskInfo] = summon[Encoder[HackSubtaskInfo]]

  /** 从路径解析目标提交 id 和正整数子任务下标。 */
  override def decode(request: Request[IO], pathParams: PathParams): IO[(SubmissionId, Int)] =
    val _ = request
    for
      submissionId <- HttpApiError.fromEitherBadRequest(pathParams.require("submissionId").flatMap(SubmissionId.parse))
      subtaskIndex <- HttpApiError.fromEitherBadRequest(
        pathParams.require("subtaskIndex").flatMap(raw => raw.toIntOption.filter(_ > 0).toRight("Subtask index is invalid."))
      )
    yield submissionId -> subtaskIndex

  /** 加载并校验目标子任务可 hack 后返回创建 hack 所需信息。 */
  override def plan(connection: Connection, actor: AuthenticatedUser, input: (SubmissionId, Int)): IO[HackSubtaskInfo] =
    val (submissionId, subtaskIndex) = input
    HackApiSupport
      .loadTargetContext(connection, actor, submissionId, subtaskIndex, submissionProgramStorage, problemDataStorage)
      .flatMap(HackApiSupport.subtaskInfo)
