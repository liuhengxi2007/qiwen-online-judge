package domains.judge.http

import cats.effect.IO
import domains.judge.application.JudgeConfig
import domains.shared.http.InternalTokenHttpSupport
import org.http4s.{Request, Response}

object JudgeHttpSupport:

  def withJudgeToken(
    request: Request[IO],
    judgeConfig: JudgeConfig
  )(handle: => IO[Response[IO]]): IO[Response[IO]] =
    InternalTokenHttpSupport.withJudgeToken(request, judgeConfig.sharedToken, JudgeHttpResponses.unauthorizedResponse)(handle)
