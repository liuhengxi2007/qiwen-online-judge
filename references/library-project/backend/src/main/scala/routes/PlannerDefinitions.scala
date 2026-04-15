package routes

import api.{EchoPlanner, SaveDemoNotePlanner}

object PlannerDefinitions:

  import PlannerRegistry.RegisteredPlan.{Plain, WithConnection}

  val planners: Map[String, PlannerRegistry.RegisteredPlan] =
    List(
      Plain(EchoPlanner),
      WithConnection(SaveDemoNotePlanner)
    ).map(planner => planner.name -> planner).toMap
