package domains.judge.http

import cats.effect.IO
import domains.judge.application.JudgeConfig
import domains.judge.http.utils.JudgeHttpSupport
import munit.CatsEffectSuite
import org.http4s.{Header, Request, Response, Status}
import org.typelevel.ci.CIString

class JudgeHttpSupportSuite extends CatsEffectSuite:

  private val config = JudgeConfig(
    sharedToken = "secret-token",
    heartbeatIntervalMs = 5000L,
    heartbeatTimeoutMs = 15000L
  )

  test("withJudgeToken authorizes requests with the configured token") {
    val request = Request[IO]().putHeaders(Header.Raw(CIString("x-judge-token"), "secret-token"))

    JudgeHttpSupport
      .withJudgeToken(request, config)(IO.pure(Response[IO](status = Status.Ok)))
      .map(response => assertEquals(response.status, Status.Ok))
  }

  test("withJudgeToken rejects requests with a missing token") {
    val request = Request[IO]()

    JudgeHttpSupport
      .withJudgeToken(request, config)(IO.pure(Response[IO](status = Status.Ok)))
      .map(response => assertEquals(response.status, Status.Unauthorized))
  }

  test("withJudgeToken rejects requests with the wrong token value") {
    val request = Request[IO]().putHeaders(Header.Raw(CIString("x-judge-token"), "wrong-token"))

    JudgeHttpSupport
      .withJudgeToken(request, config)(IO.pure(Response[IO](status = Status.Ok)))
      .map(response => assertEquals(response.status, Status.Unauthorized))
  }

  test("withJudgeToken ignores unrelated headers when the judge token is absent") {
    val request = Request[IO]().putHeaders(Header.Raw(CIString("x-request-id"), "abc"))

    JudgeHttpSupport
      .withJudgeToken(request, config)(IO.pure(Response[IO](status = Status.Ok)))
      .map(response => assertEquals(response.status, Status.Unauthorized))
  }
