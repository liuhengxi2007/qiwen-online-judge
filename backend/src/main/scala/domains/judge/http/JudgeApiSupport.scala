package domains.judge.http

import cats.effect.IO
import domains.judge.utils.JudgeConfig
import org.http4s.Request
import org.typelevel.ci.CIString
import shared.http.{ApiMessages, HttpApiError}

object JudgeApiSupport:

  private val judgeTokenHeader = CIString("x-judge-token")

  def ensureJudgeToken(request: Request[IO], judgeConfig: JudgeConfig): IO[Unit] =
    val providedToken = request.headers.headers.find(_.name == judgeTokenHeader).map(_.value)
    HttpApiError.ensure(
      providedToken.contains(judgeConfig.sharedToken),
      HttpApiError.unauthorized(ApiMessages.judgeTokenInvalid)
    )
