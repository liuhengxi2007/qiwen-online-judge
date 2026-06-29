package domains.hack.api

import cats.effect.IO
import domains.auth.api.AuthenticatedApi
import domains.auth.objects.internal.AuthenticatedUser
import domains.hack.objects.response.SubmissionHackAvailability
import domains.problem.api.ProblemDataStorageContext
import domains.submission.objects.SubmissionId
import domains.submission.api.SubmissionProgramStorageContext
import io.circe.Encoder
import org.http4s.{Method, Request, Status}
import shared.api.{ApiPath, HttpApiError, PathParams}

import java.sql.Connection

/** 查询提交各子任务 hack 可用性的认证 API；需要用户能查看目标提交详情。 */
final case class GetSubmissionHackAvailability(
  submissionProgramStorage: SubmissionProgramStorageContext,
  problemDataStorage: ProblemDataStorageContext
) extends AuthenticatedApi[SubmissionId, SubmissionHackAvailability]:

  override val method: Method = Method.GET
  override val path: ApiPath = ApiPath("/api/submissions/:submissionId/hack/availability")
  override val successStatus: Status = Status.Ok
  override protected val outputEncoder: Encoder[SubmissionHackAvailability] = summon[Encoder[SubmissionHackAvailability]]

  /** 从路径解析目标提交 public id。 */
  override def decode(request: Request[IO], pathParams: PathParams): IO[SubmissionId] =
    val _ = request
    HttpApiError.fromEitherBadRequest(pathParams.require("submissionId").flatMap(SubmissionId.parse))

  /** 构建目标提交的 JudgeTask 并返回每个子任务是否可 hack。 */
  override def plan(connection: Connection, actor: AuthenticatedUser, submissionId: SubmissionId): IO[SubmissionHackAvailability] =
    HackApiSupport
      .loadTargetTask(connection, actor, submissionId, submissionProgramStorage, problemDataStorage)
      .map(HackApiSupport.hackAvailability)
