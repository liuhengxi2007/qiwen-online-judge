package domains.judger.http

import domains.judger.http.mapper.JudgerRegistryHttpResponseMappers
import domains.judger.http.mapper.JudgerRegistryHttpRequestMappers



import domains.judger.http.utils.JudgerRegistryHttpSupport
import cats.effect.IO
import database.DatabaseSession
import domains.auth.application.SessionStore
import domains.auth.http.utils.AuthHttpSessionSupport
import domains.judge.application.JudgeConfig
import domains.judger.application.JudgerRegistryCommands
import judgeprotocol.objects.{JudgerHeartbeatRequest, RegisterJudgerRequest}
import org.http4s.{Request, Response}
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.dsl.Http4sDsl

final class JudgerRegistryHttpHandlers(
  databaseSession: DatabaseSession,
  judgeConfig: JudgeConfig,
  sessionStore: SessionStore
)(using dsl: Http4sDsl[IO]):


  def register(request: Request[IO]): IO[Response[IO]] =
    JudgerRegistryHttpSupport.withJudgeToken(request, judgeConfig) {
      for
        registerRequest <- request.as[RegisterJudgerRequest]
        response <- JudgerRegistryCommands
          .register(databaseSession, judgeConfig, registerRequest)
          .flatMap(JudgerRegistryHttpResponseMappers.mapRegisterResult)
      yield response
    }

  def heartbeat(request: Request[IO], rawJudgerId: String): IO[Response[IO]] =
    JudgerRegistryHttpSupport.withJudgeToken(request, judgeConfig) {
      JudgerRegistryHttpRequestMappers.judgerId(rawJudgerId) match
        case Left(message) =>
          JudgerRegistryHttpResponseMappers.validationErrorResponse(message)
        case Right(judgerId) =>
          for
            _ <- request.as[JudgerHeartbeatRequest]
            response <- JudgerRegistryCommands
              .heartbeat(databaseSession, judgeConfig, judgerId)
              .flatMap(JudgerRegistryHttpResponseMappers.mapHeartbeatResult)
          yield response
    }

  def listRegistered(request: Request[IO]): IO[Response[IO]] =
    AuthHttpSessionSupport.withSiteManager(databaseSession, sessionStore, request) { actor =>
      val _ = actor
      JudgerRegistryCommands
        .listRegistered(databaseSession, judgeConfig)
        .flatMap(JudgerRegistryHttpResponseMappers.listRegisteredJudgersResponse)
    }
