package domains.judger.http.utils



import domains.judger.http.mapper.JudgerRegistryHttpResponseMappers
import cats.effect.IO
import domains.judge.application.JudgeConfig
import shared.http.utils.InternalTokenHttpSupport
import org.http4s.{Request, Response}

object JudgerRegistryHttpSupport:

  def withJudgeToken(
    request: Request[IO],
    judgeConfig: JudgeConfig
  )(handle: => IO[Response[IO]]): IO[Response[IO]] =
    InternalTokenHttpSupport.withJudgeToken(request, judgeConfig.sharedToken, JudgerRegistryHttpResponseMappers.unauthorizedResponse)(handle)
