package domains.judge.http

import cats.effect.IO
import database.DatabaseSession
import domains.judge.application.{JudgeCommands, JudgeConfig}
import judgeprotocol.model.{ClaimJudgeTaskRequest, ReportJudgeResultRequest}
import domains.submission.model.SubmissionId
import org.http4s.HttpRoutes
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.dsl.io.*
import org.typelevel.ci.CIString

object JudgeRouter:

  def routes(databaseSession: DatabaseSession, judgeConfig: JudgeConfig): HttpRoutes[IO] =
    HttpRoutes.of[IO] {
      case request @ POST -> Root / "api" / "internal" / "judge" / "claim" =>
        withJudgeToken(request, judgeConfig) {
          for
            claimRequest <- request.as[ClaimJudgeTaskRequest]
            response <- JudgeCommands
              .claimCpp17Task(databaseSession, claimRequest.judgerName)
              .flatMap(JudgeHttpResponses.mapClaimResult)
          yield response
        }

      case request @ POST -> Root / "api" / "internal" / "judge" / "submissions" / rawSubmissionId / "complete" =>
        withJudgeToken(request, judgeConfig) {
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
    }

  private def withJudgeToken(
    request: org.http4s.Request[IO],
    judgeConfig: JudgeConfig
  )(handle: => IO[org.http4s.Response[IO]]): IO[org.http4s.Response[IO]] =
    val providedToken = request.headers.headers.find(_.name == CIString("x-judge-token")).map(_.value)
    if providedToken.contains(judgeConfig.sharedToken) then handle
    else JudgeHttpResponses.unauthorizedResponse
