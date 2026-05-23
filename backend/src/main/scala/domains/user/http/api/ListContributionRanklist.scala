package domains.user.http.api



import domains.user.http.*
import domains.user.http.codec.UserHttpCodecs.given
import cats.effect.IO
import domains.user.model.Username
import shared.model.PageRequest
import domains.user.application.UserMutationCommands
import domains.user.http.UserHttpPlanDefinitions.{deleteUser, getUserProfile, getUserSettings, listAcceptedRanklist, listContributionRanklist, listUserSuggestions, listUsers, updateUserPermissions}
import domains.user.application.input.{UpdateUserPermissionsRequest, UserListRequest, UserSearchQuery}
import org.http4s.HttpRoutes
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.dsl.Http4sDsl
import org.http4s.dsl.io.*

object ListContributionRanklist:

  def routes(context: UserHttpRouteContext)(using Http4sDsl[IO]): HttpRoutes[IO] =
    HttpRoutes.of[IO] {
      case request @ GET -> Root / "api" / "users" / "ranklist" =>
        context.handlers.execute(request, PageRequest(page = parsePage(request.uri.query.params.get("page"))), listContributionRanklist)
    }

  private def parsePage(rawPage: Option[String]): Int =
    rawPage.flatMap(_.toIntOption).getOrElse(1)

  private def parsePageSize(rawPageSize: Option[String]): Int =
    rawPageSize.flatMap(_.toIntOption).filter(_ > 0).getOrElse(10)
