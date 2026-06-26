package domains.judge.api

import cats.effect.IO
import org.http4s.Request
import org.typelevel.ci.CIString
import shared.api.{ApiMessages, HttpApiError}

/** judge worker 共享 token 校验工具；用于 worker 公开接口的轻量认证。 */
object JudgeTokenAuth:
  private val judgeTokenHeader = CIString("x-judge-token")

  /** 校验 x-judge-token 是否与配置一致；失败返回未授权错误。 */
  def ensureJudgeToken(request: Request[IO], judgeConfig: JudgeConfig): IO[Unit] =
    val providedToken = request.headers.headers.find(_.name == judgeTokenHeader).map(_.value)
    HttpApiError.ensure(
      providedToken.contains(judgeConfig.sharedToken),
      HttpApiError.unauthorized(ApiMessages.judgeTokenInvalid)
    )
