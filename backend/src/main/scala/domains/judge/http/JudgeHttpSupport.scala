package domains.judge.http

import cats.effect.IO
import domains.judge.application.JudgeConfig
import org.http4s.{Request, Response}
import org.typelevel.ci.CIString

object JudgeHttpSupport:

  def withJudgeToken(
    request: Request[IO],
    judgeConfig: JudgeConfig
  )(handle: => IO[Response[IO]]): IO[Response[IO]] =
    val providedToken = request.headers.headers.find(_.name == CIString("x-judge-token")).map(_.value)
    if providedToken.contains(judgeConfig.sharedToken) then handle
    else JudgeHttpResponses.unauthorizedResponse
