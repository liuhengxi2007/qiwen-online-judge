package domains.hack.api

import cats.effect.IO
import domains.auth.api.AuthenticatedApi
import domains.auth.objects.internal.AuthenticatedUser
import domains.hack.objects.response.HackSubtaskInfo
import domains.problem.utils.ProblemDataStorage
import domains.submission.objects.SubmissionId
import domains.submission.utils.SubmissionProgramStorage
import io.circe.Encoder
import org.http4s.{Method, Request, Status}
import shared.api.{ApiPath, HttpApiError, PathParams}

import java.sql.Connection

final case class GetSubmissionHackSubtask(
  submissionProgramStorage: SubmissionProgramStorage,
  problemDataStorage: ProblemDataStorage
) extends AuthenticatedApi[(SubmissionId, Int), HackSubtaskInfo]:

  override val method: Method = Method.GET
  override val path: ApiPath = ApiPath("/api/submissions/:submissionId/hack/subtasks/:subtaskIndex")
  override val successStatus: Status = Status.Ok
  override protected val outputEncoder: Encoder[HackSubtaskInfo] = summon[Encoder[HackSubtaskInfo]]

  override def decode(request: Request[IO], pathParams: PathParams): IO[(SubmissionId, Int)] =
    val _ = request
    for
      submissionId <- HttpApiError.fromEitherBadRequest(pathParams.require("submissionId").flatMap(SubmissionId.parse))
      subtaskIndex <- HttpApiError.fromEitherBadRequest(
        pathParams.require("subtaskIndex").flatMap(raw => raw.toIntOption.filter(_ > 0).toRight("Subtask index is invalid."))
      )
    yield submissionId -> subtaskIndex

  override def plan(connection: Connection, actor: AuthenticatedUser, input: (SubmissionId, Int)): IO[HackSubtaskInfo] =
    val (submissionId, subtaskIndex) = input
    HackApiSupport
      .loadTargetContext(connection, actor, submissionId, subtaskIndex, submissionProgramStorage, problemDataStorage)
      .map(HackApiSupport.subtaskInfo)
