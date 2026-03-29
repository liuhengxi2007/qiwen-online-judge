package routes

import api.{ConnectionApiPlan, PlainApiPlan}
import io.circe.{Decoder, Encoder}

object PlannerRegistry:

  sealed trait RegisteredPlan:
    def name: String

  object RegisteredPlan:

    final case class Plain[Input, Output](
      planner: PlainApiPlan[Input, Output]
    )(using val inputDecoder: Decoder[Input], val outputEncoder: Encoder[Output]) extends RegisteredPlan:
      override val name: String = planner.name

    final case class WithConnection[Input, Output](
      planner: ConnectionApiPlan[Input, Output]
    )(using val inputDecoder: Decoder[Input], val outputEncoder: Encoder[Output]) extends RegisteredPlan:
      override val name: String = planner.name

  val planners: Map[String, RegisteredPlan] = PlannerDefinitions.planners
