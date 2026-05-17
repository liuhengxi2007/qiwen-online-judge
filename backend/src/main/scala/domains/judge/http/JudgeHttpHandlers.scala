package domains.judge.http

import cats.effect.{Clock, IO}
import database.DatabaseSession
import domains.judge.application.{JudgeCommands, JudgeConfig}
import domains.problem.application.ProblemDataStorage
import domains.problem.model.{ProblemDataPath, ProblemSlug}
import domains.submission.model.SubmissionId
import judgeprotocol.model.{ClaimJudgeTaskRequest, ReportJudgeResultRequest}
import org.http4s.{Request, Response}
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.dsl.Http4sDsl

final class JudgeHttpHandlers(
  databaseSession: DatabaseSession,
  judgeConfig: JudgeConfig,
  problemDataStorage: ProblemDataStorage
)(using dsl: Http4sDsl[IO]):

  import dsl.*

  def claim(request: Request[IO]): IO[Response[IO]] =
    JudgeHttpSupport.withJudgeToken(request, judgeConfig) {
      for
        claimRequest <- request.as[ClaimJudgeTaskRequest]
        claimedAt <- Clock[IO].realTimeInstant
        response <- JudgeCommands
          .claimTask(databaseSession, judgeConfig, claimRequest.judgerId, claimedAt)
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
            completedAt <- Clock[IO].realTimeInstant
            response <- JudgeCommands
              .reportJudgeResult(databaseSession, submissionId, resultRequest, completedAt)
              .flatMap(JudgeHttpResponses.mapReportResult)
          yield response
    }

  def downloadProblemData(request: Request[IO]): IO[Response[IO]] =
    JudgeHttpSupport.withJudgeToken(request, judgeConfig) {
      val maybeProblemSlug = request.uri.query.params.get("problemSlug").flatMap(raw => ProblemSlug.parse(raw).toOption)
      val maybePath = request.uri.query.params.get("path").flatMap(raw => ProblemDataPath.parse(raw).toOption)
      (maybeProblemSlug, maybePath) match
        case (Some(problemSlug), Some(path)) =>
          JudgeProblemDataDownload.response(problemDataStorage, problemSlug, path)
        case _ =>
          JudgeHttpResponses.validationErrorResponse("Valid problemSlug and path query parameters are required.")
    }
