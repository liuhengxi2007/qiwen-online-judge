package api

import cats.effect.IO
import objects.{EchoRequest, EchoResponse}
import org.typelevel.log4cats.slf4j.Slf4jLogger

object EchoPlanner extends PlainApiPlan[EchoRequest, EchoResponse]:

  private val logger = Slf4jLogger.getLogger[IO]

  override val name: String = "EchoPlanner"

  override def plan(request: EchoRequest): IO[EchoResponse] =
    for
      _ <- logger.info(s"EchoPlanner started, message=${request.message}")
      response = EchoResponse(
        message = if request.uppercase then request.message.toUpperCase else request.message,
        transformed = request.uppercase
      )
      _ <- logger.info("EchoPlanner finished")
    yield response
