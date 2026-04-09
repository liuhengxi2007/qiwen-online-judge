package domains.judger.http

import cats.effect.IO
import database.DatabaseSession
import domains.judge.application.JudgeConfig
import domains.judger.application.JudgerRegistryCommands
import judgeprotocol.model.{JudgerHeartbeatRequest, JudgerId, RegisterJudgerRequest}
import org.http4s.HttpRoutes
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.dsl.io.*
import org.typelevel.ci.CIString

object JudgerRegistryRouter:

  def routes(databaseSession: DatabaseSession, judgeConfig: JudgeConfig): HttpRoutes[IO] =
    HttpRoutes.of[IO] {
      case request @ POST -> Root / "api" / "internal" / "judgers" / "register" =>
        withJudgeToken(request, judgeConfig) {
          for
            registerRequest <- request.as[RegisterJudgerRequest]
            response <- JudgerRegistryCommands
              .register(databaseSession, judgeConfig, registerRequest)
              .flatMap(JudgerRegistryHttpResponses.mapRegisterResult)
          yield response
        }

      case request @ POST -> Root / "api" / "internal" / "judgers" / rawJudgerId / "heartbeat" =>
        withJudgeToken(request, judgeConfig) {
          JudgerId.parse(rawJudgerId) match
            case Left(message) =>
              JudgerRegistryHttpResponses.validationErrorResponse(message)
            case Right(judgerId) =>
              for
                _ <- request.as[JudgerHeartbeatRequest]
                response <- JudgerRegistryCommands
                  .heartbeat(databaseSession, judgeConfig, judgerId)
                  .flatMap(JudgerRegistryHttpResponses.mapHeartbeatResult)
              yield response
        }
    }

  private def withJudgeToken(
    request: org.http4s.Request[IO],
    judgeConfig: JudgeConfig
  )(handle: => IO[org.http4s.Response[IO]]): IO[org.http4s.Response[IO]] =
    val providedToken = request.headers.headers.find(_.name == CIString("x-judge-token")).map(_.value)
    if providedToken.contains(judgeConfig.sharedToken) then handle
    else JudgerRegistryHttpResponses.unauthorizedResponse
