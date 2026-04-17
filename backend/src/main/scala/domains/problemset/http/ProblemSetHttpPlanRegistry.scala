package domains.problemset.http

import cats.effect.IO

object ProblemSetHttpPlanRegistry:

  sealed trait RegisteredPlan:
    def name: String

  object RegisteredPlan:

    final case class Plain[Input, Output](
      plan: PlainProblemSetHttpPlan[Input, Output],
      toResponse: Output => IO[org.http4s.Response[IO]]
    ) extends RegisteredPlan:
      override val name: String = plan.name

    final case class WithTransaction[Input, Output](
      plan: TransactionProblemSetHttpPlan[Input, Output],
      toResponse: Output => IO[org.http4s.Response[IO]]
    ) extends RegisteredPlan:
      override val name: String = plan.name
