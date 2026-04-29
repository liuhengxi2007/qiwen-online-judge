package domains.judge.http

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import domains.judge.application.JudgeConfig
import munit.FunSuite
import org.http4s.{Header, Request, Response, Status}
import org.typelevel.ci.CIString

class JudgeHttpSupportSuite extends FunSuite:

  private val config = JudgeConfig(
    sharedToken = "secret-token",
    heartbeatIntervalMs = 5000L,
    heartbeatTimeoutMs = 15000L
  )

  test("withJudgeToken authorizes requests with the configured token") {
    val request = Request[IO]().putHeaders(Header.Raw(CIString("x-judge-token"), "secret-token"))

    val response = JudgeHttpSupport
      .withJudgeToken(request, config)(IO.pure(Response[IO](status = Status.Ok)))
      .unsafeRunSync()

    assertEquals(response.status, Status.Ok)
  }

  test("withJudgeToken rejects requests with a missing token") {
    val request = Request[IO]()

    val response = JudgeHttpSupport
      .withJudgeToken(request, config)(IO.pure(Response[IO](status = Status.Ok)))
      .unsafeRunSync()

    assertEquals(response.status, Status.Unauthorized)
  }
