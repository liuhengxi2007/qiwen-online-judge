package domains.submission.http

import cats.effect.IO

object SubmissionHttpPlanRegistry:

  sealed trait RegisteredPlan:
    def name: String

  object RegisteredPlan:

    final case class Plain[Input, Output](
      plan: PlainSubmissionHttpPlan[Input, Output],
      toResponse: Output => IO[org.http4s.Response[IO]]
    ) extends RegisteredPlan:
      override val name: String = plan.name

    final case class WithTransaction[Input, Output](
      plan: TransactionSubmissionHttpPlan[Input, Output],
      toResponse: Output => IO[org.http4s.Response[IO]]
    ) extends RegisteredPlan:
      override val name: String = plan.name
