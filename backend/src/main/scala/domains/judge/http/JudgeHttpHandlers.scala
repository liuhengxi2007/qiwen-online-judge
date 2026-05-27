package domains.judge.http

import domains.judge.http.mapper.JudgeHttpResponseMappers
import domains.judge.http.mapper.JudgeHttpRequestMappers

import domains.judge.http.utils.JudgeHttpSupport
import cats.effect.{Clock, IO}
import database.DatabaseSession
import domains.judge.application.{JudgeCommands, JudgeConfig}
import domains.problem.application.ProblemDataStorage
import judgeprotocol.objects.{ClaimJudgeTaskRequest, ReportJudgeResultRequest}
import org.http4s.{Request, Response}
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.dsl.Http4sDsl

final class JudgeHttpHandlers(
  databaseSession: DatabaseSession,
  judgeConfig: JudgeConfig,
  problemDataStorage: ProblemDataStorage
)(using dsl: Http4sDsl[IO]):


  def claim(request: Request[IO]): IO[Response[IO]] =
    JudgeHttpSupport.withJudgeToken(request, judgeConfig) {
      for
        claimRequest <- request.as[ClaimJudgeTaskRequest]
        claimedAt <- Clock[IO].realTimeInstant
        response <- JudgeCommands
          .claimTask(databaseSession, judgeConfig, problemDataStorage, claimRequest.judgerId, claimedAt)
          .flatMap(JudgeHttpResponseMappers.mapClaimResult)
      yield response
    }

  def completeSubmission(request: Request[IO], rawSubmissionId: String): IO[Response[IO]] =
    JudgeHttpSupport.withJudgeToken(request, judgeConfig) {
      JudgeHttpRequestMappers.submissionId(rawSubmissionId) match
        case Left(message) =>
          JudgeHttpResponseMappers.validationErrorResponse(message)
        case Right(submissionId) =>
          for
            resultRequest <- request.as[ReportJudgeResultRequest]
            completedAt <- Clock[IO].realTimeInstant
            response <- JudgeCommands
              .reportJudgeResult(databaseSession, submissionId, resultRequest, completedAt)
              .flatMap(JudgeHttpResponseMappers.mapReportResult)
          yield response
    }

  def downloadProblemData(request: Request[IO]): IO[Response[IO]] =
    JudgeHttpSupport.withJudgeToken(request, judgeConfig) {
      JudgeHttpRequestMappers.problemDataDownloadInput(request.uri.query.params) match
        case Right((problemSlug, path)) =>
          JudgeProblemDataDownload.response(problemDataStorage, problemSlug, path)
        case Left(message) =>
          JudgeHttpResponseMappers.validationErrorResponse(message)
    }
