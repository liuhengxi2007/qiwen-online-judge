package api

import cats.effect.IO
import io.circe.{Decoder, Encoder}

import java.sql.Connection

trait ApiPlan[Input, Output]:

  def name: String

trait PlainApiPlan[Input: Decoder, Output: Encoder] extends ApiPlan[Input, Output]:

  def plan(input: Input): IO[Output]

trait ConnectionApiPlan[Input: Decoder, Output: Encoder] extends ApiPlan[Input, Output]:

  def plan(input: Input, connection: Connection): IO[Output]
