package domains.auth.http



import cats.effect.IO

object AuthHttpPlanRegistry:

  sealed trait RegisteredPlan:
    def name: String

  object RegisteredPlan:

    final case class PublicPlain[Input, Output](
      plan: PublicPlainAuthHttpPlan[Input, Output],
      toResponse: Output => IO[org.http4s.Response[IO]]
    ) extends RegisteredPlan:
      override val name: String = plan.name

    final case class PublicWithTransaction[Input, Output](
      plan: PublicTransactionAuthHttpPlan[Input, Output],
      toResponse: Output => IO[org.http4s.Response[IO]]
    ) extends RegisteredPlan:
      override val name: String = plan.name

    final case class AuthenticatedPlain[Input, Output](
      plan: AuthenticatedPlainAuthHttpPlan[Input, Output],
      toResponse: Output => IO[org.http4s.Response[IO]]
    ) extends RegisteredPlan:
      override val name: String = plan.name

    final case class AuthenticatedWithTransaction[Input, Output](
      plan: AuthenticatedTransactionAuthHttpPlan[Input, Output],
      toResponse: Output => IO[org.http4s.Response[IO]]
    ) extends RegisteredPlan:
      override val name: String = plan.name

    final case class SiteManagerPlain[Input, Output](
      plan: SiteManagerPlainAuthHttpPlan[Input, Output],
      toResponse: Output => IO[org.http4s.Response[IO]]
    ) extends RegisteredPlan:
      override val name: String = plan.name

    final case class SiteManagerWithTransaction[Input, Output](
      plan: SiteManagerTransactionAuthHttpPlan[Input, Output],
      toResponse: Output => IO[org.http4s.Response[IO]]
    ) extends RegisteredPlan:
      override val name: String = plan.name
