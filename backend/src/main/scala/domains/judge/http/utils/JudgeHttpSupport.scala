package domains.judge.http.utils

import domains.judge.http.mapper.JudgeHttpResponseMappers
import cats.effect.IO
import domains.judge.application.JudgeConfig
import shared.http.utils.InternalTokenHttpSupport
import org.http4s.{Request, Response}

object JudgeHttpSupport:

  def withJudgeToken(
    request: Request[IO],
    judgeConfig: JudgeConfig
  )(handle: => IO[Response[IO]]): IO[Response[IO]] =
    InternalTokenHttpSupport.withJudgeToken(request, judgeConfig.sharedToken, JudgeHttpResponseMappers.unauthorizedResponse)(handle)
