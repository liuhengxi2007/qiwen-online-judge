package domains.shared.http.utils



import cats.effect.IO
import org.http4s.{Request, Response}
import org.typelevel.ci.CIString

object InternalTokenHttpSupport:

  private val judgeTokenHeader = CIString("x-judge-token")

  def withJudgeToken(
    request: Request[IO],
    expectedToken: String,
    unauthorizedResponse: => IO[Response[IO]]
  )(handle: => IO[Response[IO]]): IO[Response[IO]] =
    val providedToken = request.headers.headers.find(_.name == judgeTokenHeader).map(_.value)
    if providedToken.contains(expectedToken) then handle
    else unauthorizedResponse
