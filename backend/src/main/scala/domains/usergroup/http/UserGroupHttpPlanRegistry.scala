package domains.usergroup.http

import cats.effect.IO

object UserGroupHttpPlanRegistry:

  sealed trait RegisteredPlan:
    def name: String

  object RegisteredPlan:

    final case class Plain[Input, Output](
      plan: PlainUserGroupHttpPlan[Input, Output],
      toResponse: Output => IO[org.http4s.Response[IO]]
    ) extends RegisteredPlan:
      override val name: String = plan.name

    final case class WithTransaction[Input, Output](
      plan: TransactionUserGroupHttpPlan[Input, Output],
      toResponse: Output => IO[org.http4s.Response[IO]]
    ) extends RegisteredPlan:
      override val name: String = plan.name
