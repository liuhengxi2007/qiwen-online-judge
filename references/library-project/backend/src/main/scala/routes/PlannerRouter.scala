package routes

import cats.effect.IO
import cats.syntax.all.*
import database.DatabaseSession
import io.circe.Json
import io.circe.syntax.*
import objects.ErrorResponse
import org.http4s.HttpRoutes
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.circe.CirceEntityEncoder.*
import org.http4s.dsl.io.*
import org.typelevel.log4cats.slf4j.Slf4jLogger

object PlannerRouter:

  private val logger = Slf4jLogger.getLogger[IO]

  private def decodeInput[Input](plannerName: String, payload: Json)(using decoder: io.circe.Decoder[Input]): IO[Input] =
    IO.fromEither(
      payload.as[Input].left.map(error =>
        new IllegalArgumentException(s"Invalid JSON for $plannerName: ${error.getMessage}")
      )
    )

  private def executePlanner(plannerName: String, payload: Json): IO[Json] =
    PlannerRegistry.planners
      .get(plannerName)
      .liftTo[IO](new IllegalArgumentException(s"Unknown planner: $plannerName"))
      .flatMap {
        case registered: PlannerRegistry.RegisteredPlan.Plain[input, output] =>
          for
            input <- decodeInput(registered.name, payload)(using registered.inputDecoder)
            output <- registered.planner.plan(input)
          yield registered.outputEncoder(output)

        case registered: PlannerRegistry.RegisteredPlan.WithConnection[input, output] =>
          for
            input <- decodeInput(registered.name, payload)(using registered.inputDecoder)
            output <- DatabaseSession.withTransactionConnection(connection =>
              registered.planner.plan(input, connection)
            )
          yield registered.outputEncoder(output)
      }

  val routes: HttpRoutes[IO] = HttpRoutes.of[IO] {
    case req @ POST -> Root / "api" / plannerName =>
      (
        for
          _ <- logger.info(s"PlannerRouter received POST /api/$plannerName")
          bodyJson <- req.as[Json]
          responseJson <- executePlanner(plannerName, bodyJson)
          response <- Ok(responseJson)
        yield response
      ).handleErrorWith { error =>
        for
          _ <- logger.error(error)(s"PlannerRouter failed: ${error.getMessage}")
          response <- BadRequest(ErrorResponse(error.getMessage).asJson)
        yield response
      }
  }
