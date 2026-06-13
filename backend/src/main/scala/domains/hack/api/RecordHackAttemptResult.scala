package domains.hack.api

import cats.effect.IO
import cats.syntax.all.*
import domains.auth.api.InternalOnlyApi
import domains.hack.objects.{HackId, HackStatus}
import domains.hack.table.hack.HackMutationTable
import domains.problem.api.MaterializeHackProblemData
import domains.problem.objects.{ProblemDataPath, ProblemId}
import domains.problem.utils.ProblemDataStorageContext
import judgeprotocol.objects.request.ReportHackResultRequest
import org.http4s.Method
import shared.api.ApiPath

import java.sql.Connection

/** 内部记录 hack attempt 结果的输入；包含 hack id 和 worker 上报结果。 */
final case class RecordHackAttemptResultInput(
  hackId: HackId,
  request: ReportHackResultRequest
)

/** 内部记录 hack 结果 API；成功 hack 会物化为题目数据并返回受影响题目 id。 */
final case class RecordHackAttemptResult(problemDataStorage: ProblemDataStorageContext) extends InternalOnlyApi[RecordHackAttemptResultInput, Option[ProblemId]]:

  override val method: Method = Method.POST
  override val path: ApiPath = ApiPath("/api/internal/hacks/judge/result")

  /** 写入 hack 终态；仅 success 且成功更新 running attempt 时追加题目数据。 */
  override def plan(connection: Connection, input: RecordHackAttemptResultInput): IO[Option[ProblemId]] =
    for
      finishedAt <- IO.realTimeInstant
      status <- IO.fromEither(HackStatus.parse(input.request.status).left.map(IllegalArgumentException(_)))
      _ <- IO.raiseUnless(HackStatus.isTerminal(status))(IllegalArgumentException("Hack result status must be terminal."))
      _ <- RecordHackAttemptResult.validateAnswerSize(input.request.answer)
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
            inputPath <- RecordHackAttemptResult.problemDataPath(RecordHackAttemptResult.inputPathFor(input.hackId))
            answerPath <- input.request.answer.traverse(_ => RecordHackAttemptResult.problemDataPath(RecordHackAttemptResult.answerPathFor(input.hackId)))
            _ <- MaterializeHackProblemData(problemDataStorage).plan(
              connection,
              MaterializeHackProblemData.input(
                problemId = source.problemId,
                problemSlug = source.problemSlug,
                subtaskIndex = source.subtaskIndex,
                inputPath = inputPath,
                answerPath = answerPath,
                testcaseLabel = s"hack #${input.hackId.value}",
                inputText = source.input,
                answerText = input.request.answer,
                createdAt = finishedAt
              )
            )
          yield Some(source.problemId)
        case _ =>
          IO.pure(None)
    yield problemId

/** hack 结果记录的构造和路径命名工具。 */
object RecordHackAttemptResult:
  private val maxAnswerBytes: Int = 2 * 1024 * 1024

  /** 构造内部记录结果输入。 */
  def input(hackId: HackId, request: ReportHackResultRequest): RecordHackAttemptResultInput =
    RecordHackAttemptResultInput(hackId = hackId, request = request)

  private def problemDataPath(value: String): IO[ProblemDataPath] =
    IO.fromEither(ProblemDataPath.parse(value).left.map(IllegalArgumentException(_)))

  private def inputPathFor(hackId: HackId): String =
    s"hacks/${hackId.value}.in"

  private def answerPathFor(hackId: HackId): String =
    s"hacks/${hackId.value}.ans"

  private def validateAnswerSize(answer: Option[String]): IO[Unit] =
    answer match
      case Some(value) if value.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > maxAnswerBytes =>
        IO.raiseError(IllegalArgumentException(s"Hack answer must be at most ${maxAnswerBytes / 1024 / 1024} MB."))
      case _ =>
        IO.unit
