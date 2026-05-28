package domains.judge.utils

import cats.effect.IO
import munit.CatsEffectSuite
import org.http4s.{Header, Request, Status}
import org.typelevel.ci.CIString
import shared.api.HttpApiError

class JudgeTokenAuthSuite extends CatsEffectSuite:

  private val config = JudgeConfig(
    sharedToken = "secret-token",
    heartbeatIntervalMs = 5000L,
    heartbeatTimeoutMs = 15000L
  )

  test("ensureJudgeToken authorizes requests with the configured token") {
    val request = Request[IO]().putHeaders(Header.Raw(CIString("x-judge-token"), "secret-token"))

    JudgeTokenAuth.ensureJudgeToken(request, config).map(result => assertEquals(result, ()))
  }

  test("ensureJudgeToken rejects requests with a missing token") {
    val request = Request[IO]()

    JudgeTokenAuth.ensureJudgeToken(request, config).attempt.map {
      case Left(error: HttpApiError) => assertEquals(error.status, Status.Unauthorized)
      case other => fail(s"Expected unauthorized HttpApiError, got $other")
    }
  }

  test("ensureJudgeToken rejects requests with the wrong token value") {
    val request = Request[IO]().putHeaders(Header.Raw(CIString("x-judge-token"), "wrong-token"))

    JudgeTokenAuth.ensureJudgeToken(request, config).attempt.map {
      case Left(error: HttpApiError) => assertEquals(error.status, Status.Unauthorized)
      case other => fail(s"Expected unauthorized HttpApiError, got $other")
    }
  }

  test("ensureJudgeToken ignores unrelated headers when the judge token is absent") {
    val request = Request[IO]().putHeaders(Header.Raw(CIString("x-request-id"), "abc"))

    JudgeTokenAuth.ensureJudgeToken(request, config).attempt.map {
      case Left(error: HttpApiError) => assertEquals(error.status, Status.Unauthorized)
      case other => fail(s"Expected unauthorized HttpApiError, got $other")
    }
  }
