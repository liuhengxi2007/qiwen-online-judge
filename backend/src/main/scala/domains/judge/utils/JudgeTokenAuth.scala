package domains.judge.utils

import cats.effect.IO
import org.http4s.Request
import org.typelevel.ci.CIString
import shared.api.{ApiMessages, HttpApiError}

object JudgeTokenAuth:
  private val judgeTokenHeader = CIString("x-judge-token")

  def ensureJudgeToken(request: Request[IO], judgeConfig: JudgeConfig): IO[Unit] =
    val providedToken = request.headers.headers.find(_.name == judgeTokenHeader).map(_.value)
    HttpApiError.ensure(
      providedToken.contains(judgeConfig.sharedToken),
      HttpApiError.unauthorized(ApiMessages.judgeTokenInvalid)
    )
