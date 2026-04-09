package domains.judger.application

import cats.effect.IO
import database.DatabaseSession
import domains.judge.application.JudgeConfig
import domains.judger.table.JudgerTable
import judgeprotocol.model.{JudgerId, RegisterJudgerRequest, RegisterJudgerResponse}

object JudgerRegistryCommands:

  enum RegisterResult:
    case ValidationFailed(message: String)
    case Registered(response: RegisterJudgerResponse)

  enum HeartbeatResult:
    case ValidationFailed(message: String)
    case JudgerNotFound
    case Updated

  def register(
    databaseSession: DatabaseSession,
    judgeConfig: JudgeConfig,
    request: RegisterJudgerRequest
  ): IO[RegisterResult] =
    validateRegisterRequest(request) match
      case Left(message) =>
        IO.pure(RegisterResult.ValidationFailed(message))
      case Right(validatedRequest) =>
        databaseSession.withTransactionConnection { connection =>
          JudgerTable
            .register(connection, validatedRequest, judgeConfig.heartbeatIntervalMs, judgeConfig.heartbeatTimeoutMs)
            .map(RegisterResult.Registered(_))
        }

  def heartbeat(
    databaseSession: DatabaseSession,
    judgeConfig: JudgeConfig,
    judgerId: JudgerId
  ): IO[HeartbeatResult] =
    databaseSession.withTransactionConnection { connection =>
      JudgerTable.heartbeat(connection, judgerId, judgeConfig.heartbeatTimeoutMs).map {
        case true => HeartbeatResult.Updated
        case false => HeartbeatResult.JudgerNotFound
      }
    }

  private def validateRegisterRequest(request: RegisterJudgerRequest): Either[String, RegisterJudgerRequest] =
    val host = request.host.trim
    if host.isEmpty then Left("Judger host is required.")
    else if request.supportedLanguages.isEmpty then Left("Judger supported languages are required.")
    else
      Right(
        request.copy(
          host = host,
          processId = request.processId.map(_.trim).filter(_.nonEmpty)
        )
      )
