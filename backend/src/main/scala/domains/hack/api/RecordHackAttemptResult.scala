package domains.hack.api

import cats.effect.IO
import cats.syntax.all.*
import domains.auth.api.InternalOnlyApi
import domains.hack.objects.{HackId, HackStatus}
import domains.hack.table.hack.HackMutationTable
import domains.problem.api.MaterializeHackProblemData
import domains.problem.objects.{ProblemDataPath, ProblemId}
import domains.problem.utils.ProblemDataStorage
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
final case class RecordHackAttemptResult(problemDataStorage: ProblemDataStorage) extends InternalOnlyApi[RecordHackAttemptResultInput, Option[ProblemId]]:

  override val method: Method = Method.POST
  override val path: ApiPath = ApiPath("/api/internal/hacks/judge/result")

  /** 写入 hack 终态；仅 success 且成功更新 running attempt 时追加题目数据。 */
  override def plan(connection: Connection, input: RecordHackAttemptResultInput): IO[Option[ProblemId]] =
    for
      finishedAt <- IO.realTimeInstant
      status <- IO.fromEither(HackStatus.parse(input.request.status).left.map(IllegalArgumentException(_)))
      // FIXME-CN: 这里接受任意 HackStatus 并写入 finished_at；queued/running 等非终态如果由 worker 传入，也会被记录成已完成状态。
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
            // FIXME-CN: worker 返回的 answer 文本在写入题目数据前没有大小限制；异常大的答案会直接进入对象存储和 manifest。
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
  /** 构造内部记录结果输入。 */
  def input(hackId: HackId, request: ReportHackResultRequest): RecordHackAttemptResultInput =
    RecordHackAttemptResultInput(hackId = hackId, request = request)

  private def problemDataPath(value: String): IO[ProblemDataPath] =
    IO.fromEither(ProblemDataPath.parse(value).left.map(IllegalArgumentException(_)))

  private def inputPathFor(hackId: HackId): String =
    s"hacks/${hackId.value}.in"

  private def answerPathFor(hackId: HackId): String =
    s"hacks/${hackId.value}.ans"
