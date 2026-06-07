package domains.hack.api

import cats.effect.IO
import domains.auth.api.InternalOnlyApi
import domains.hack.application.HackProblemDataMaterializer
import domains.hack.objects.{HackId, HackStatus}
import domains.hack.table.hack.HackMutationTable
import domains.problem.objects.ProblemId
import domains.problem.utils.ProblemDataStorage
import judgeprotocol.objects.request.ReportHackResultRequest
import org.http4s.Method
import shared.api.ApiPath

import java.sql.Connection

final case class RecordHackAttemptResultInput(
  hackId: HackId,
  request: ReportHackResultRequest
)

final case class RecordHackAttemptResult(problemDataStorage: ProblemDataStorage) extends InternalOnlyApi[RecordHackAttemptResultInput, Option[ProblemId]]:

  override val method: Method = Method.POST
  override val path: ApiPath = ApiPath("/api/internal/hacks/judge/result")

  override def plan(connection: Connection, input: RecordHackAttemptResultInput): IO[Option[ProblemId]] =
    for
      finishedAt <- IO.realTimeInstant
      status <- IO.fromEither(HackStatus.parse(input.request.status).left.map(IllegalArgumentException(_)))
      completed <- HackMutationTable.completeAttempt(
        connection = connection,
        hackId = input.hackId,
        status = status,
        answer = input.request.answer,
        newScore = input.request.newScore,
        validatorMessage = input.request.validatorMessage,
        standardMessage = input.request.standardMessage,
        targetMessage = input.request.targetMessage,
        finishedAt = finishedAt
      )
      problemId <- (status, completed) match
        case (HackStatus.Success, Some(source)) =>
          for
            _ <- HackProblemDataMaterializer.materializeSuccessfulHack(connection, problemDataStorage, input.hackId, source, input.request.answer, finishedAt)
          yield Some(source.problemId)
        case _ =>
          IO.pure(None)
    yield problemId

object RecordHackAttemptResult:
  def input(hackId: HackId, request: ReportHackResultRequest): RecordHackAttemptResultInput =
    RecordHackAttemptResultInput(hackId = hackId, request = request)
