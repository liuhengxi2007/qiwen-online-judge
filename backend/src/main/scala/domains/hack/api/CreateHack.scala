package domains.hack.api

import cats.effect.IO
import domains.auth.api.AuthenticatedApi
import domains.auth.objects.internal.AuthenticatedUser
import domains.hack.objects.request.CreateHackRequest
import domains.hack.objects.response.HackDetail
import domains.hack.table.hack.{HackMutationTable, HackQueryTable}
import domains.problem.utils.ProblemDataStorage
import domains.submission.utils.SubmissionProgramStorage
import io.circe.Encoder
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.{Method, Request, Status}
import shared.api.{ApiPath, HttpApiError, PathParams}

import java.sql.Connection

final case class CreateHack(
  submissionProgramStorage: SubmissionProgramStorage,
  problemDataStorage: ProblemDataStorage
) extends AuthenticatedApi[CreateHackRequest, HackDetail]:

  override val method: Method = Method.POST
  override val path: ApiPath = ApiPath("/api/hacks")
  override val successStatus: Status = Status.Created
  override protected val outputEncoder: Encoder[HackDetail] = summon[Encoder[HackDetail]]

  override def decode(request: Request[IO], pathParams: PathParams): IO[CreateHackRequest] =
    val _ = pathParams
    request.as[CreateHackRequest]

  override def plan(connection: Connection, actor: AuthenticatedUser, request: CreateHackRequest): IO[HackDetail] =
    for
      context <- HackApiSupport.loadTargetContext(
        connection,
        actor,
        request.targetSubmissionId,
        request.subtaskIndex,
        submissionProgramStorage,
        problemDataStorage
      )
      _ <- HackApiSupport.validateHackText(request.input, request.strategyProviderSource, HackApiSupport.requiresStrategyProvider(context.subtask))
      hackUuid <- IO.randomUUID
      createdAt <- IO.realTimeInstant
      oldScore = HackApiSupport.targetSubtaskWorstScore(context.submission, context.subtask.index)
      normalizedStrategy = request.strategyProviderSource.filter(_.trim.nonEmpty)
      hackId <- HackMutationTable.insertAttempt(
        connection = connection,
        id = hackUuid,
        problemId = context.submission.problemId,
        targetSubmissionId = context.submission.id,
        authorUsername = actor.username,
        subtaskIndex = context.subtask.index,
        subtaskLabel = context.subtask.label,
        input = request.input,
        strategyProviderSource = normalizedStrategy,
        oldScore = oldScore,
        createdAt = createdAt
      )
      detail <- HackQueryTable.findVisibleById(connection, actor, hackId).flatMap {
        case Some(value) => IO.pure(value)
        case None => HttpApiError.raise(HttpApiError.internal("Hack attempt disappeared after creation."))
      }
    yield detail
