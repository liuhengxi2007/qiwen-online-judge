package domains.judge.http

import cats.effect.IO
import database.DatabaseSession
import domains.judge.application.{JudgeCommands, JudgeConfig}
import domains.submission.model.SubmissionId
import judgeprotocol.model.{ClaimJudgeTaskRequest, ReportJudgeResultRequest}
import org.http4s.{Request, Response}
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.dsl.Http4sDsl

final class JudgeHttpHandlers(
  databaseSession: DatabaseSession,
  judgeConfig: JudgeConfig
)(using dsl: Http4sDsl[IO]):

  import dsl.*

  def claim(request: Request[IO]): IO[Response[IO]] =
    JudgeHttpSupport.withJudgeToken(request, judgeConfig) {
      for
        claimRequest <- request.as[ClaimJudgeTaskRequest]
        response <- JudgeCommands
          .claimCpp17Task(databaseSession, claimRequest.judgerId)
          .flatMap(JudgeHttpResponses.mapClaimResult)
      yield response
    }

  def completeSubmission(request: Request[IO], rawSubmissionId: String): IO[Response[IO]] =
    JudgeHttpSupport.withJudgeToken(request, judgeConfig) {
      SubmissionId.parse(rawSubmissionId) match
        case Left(message) =>
          JudgeHttpResponses.validationErrorResponse(message)
        case Right(submissionId) =>
          for
            resultRequest <- request.as[ReportJudgeResultRequest]
            response <- JudgeCommands
              .reportJudgeResult(databaseSession, submissionId, resultRequest)
              .flatMap(JudgeHttpResponses.mapReportResult)
          yield response
    }
