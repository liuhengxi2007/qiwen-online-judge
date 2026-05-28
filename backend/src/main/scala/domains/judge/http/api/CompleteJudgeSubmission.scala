package domains.judge.http.api

import cats.effect.IO
import domains.auth.http.PublicApi
import domains.judge.utils.JudgeConfig
import domains.judge.http.JudgeApiSupport
import domains.submission.objects.SubmissionId
import domains.submission.objects.internal.SubmissionJudgeCompletion
import domains.submission.rules.SubmissionJudgeRules
import domains.submission.table.submission.{SubmissionJudgeTable, SubmissionQueryTable}
import io.circe.Encoder
import judgeprotocol.objects.ReportJudgeResultRequest
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.{Method, Request, Status}
import shared.http.codec.SharedHttpCodecs.given
import shared.http.{ApiMessages, ApiPath, HttpApiError, PathParams}
import shared.objects.response.SuccessResponse

import java.sql.Connection

final case class CompleteJudgeSubmission(judgeConfig: JudgeConfig) extends PublicApi[(SubmissionId, ReportJudgeResultRequest), SuccessResponse]:

  override val method: Method = Method.POST
  override val path: ApiPath = ApiPath("/api/internal/judge/submissions/:submissionId/complete")
  override val successStatus: Status = Status.Ok
  override protected val outputEncoder: Encoder[SuccessResponse] = summon[Encoder[SuccessResponse]]

  override def decode(request: Request[IO], pathParams: PathParams): IO[(SubmissionId, ReportJudgeResultRequest)] =
    for
      _ <- JudgeApiSupport.ensureJudgeToken(request, judgeConfig)
      submissionId <- HttpApiError.fromEitherBadRequest(pathParams.require("submissionId").flatMap(SubmissionId.parse))
      resultRequest <- request.as[ReportJudgeResultRequest]
    yield (submissionId, resultRequest)

  override def plan(connection: Connection, input: (SubmissionId, ReportJudgeResultRequest)): IO[SuccessResponse] =
    val (submissionId, request) = input
    request.status match
      case judgeprotocol.objects.SubmissionStatus.Completed | judgeprotocol.objects.SubmissionStatus.Failed =>
        for
          completedAt <- IO.realTimeInstant
          maybeSubmission <- SubmissionQueryTable.findById(connection, submissionId)
          submission <- maybeSubmission match
            case Some(submission) => IO.pure(submission)
            case None => HttpApiError.raise(HttpApiError.notFound(ApiMessages.submissionNotFound))
          completedState <- SubmissionJudgeRules
            .completeJudging(
              SubmissionJudgeRules.fromSubmissionDetail(submission),
              SubmissionJudgeCompletion(
                status = SubmissionJudgeRules.fromProtocolStatus(request.status),
                verdict = request.verdict.map(SubmissionJudgeRules.fromProtocolVerdict),
                judgeMessage = request.judgeMessage,
                timeUsedMs = request.timeUsedMs,
                memoryUsedKb = request.memoryUsedKb,
                score = request.score,
                judgeResult = request.judgeResult
              ),
              completedAt
            ) match
            case Left(message) => HttpApiError.raise(HttpApiError.badRequest(message))
            case Right(completedState) => IO.pure(completedState)
          _ <- SubmissionJudgeTable.updateJudgeState(connection, submissionId, completedState)
        yield SuccessResponse(code = Some(ApiMessages.judgeResultRecorded.code), message = None, params = ApiMessages.judgeResultRecorded.params)
      case _ =>
        HttpApiError.raise(HttpApiError.badRequest("Judge results may only set status to completed or failed."))
