package domains.user.http

import cats.effect.IO
import database.DatabaseSession
import domains.auth.application.SessionStore
import domains.auth.model.Username
import domains.shared.model.PageRequest
import domains.user.application.UserMutationCommands
import domains.user.http.UserHttpPlanDefinitions.{deleteUser, getUserProfile, getUserSettings, listAcceptedRanklist, listContributionRanklist, listUserSuggestions, listUsers, updateUserPermissions}
import domains.user.model.{UpdateUserPermissionsRequest, UserListRequest, UserSearchQuery}
import org.http4s.HttpRoutes
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.dsl.Http4sDsl
import org.http4s.dsl.io.*

object UserRouter:

  def routes(databaseSession: DatabaseSession, sessionStore: SessionStore): HttpRoutes[IO] =
    given Http4sDsl[IO] = new Http4sDsl[IO] {}
    val handlers = new UserHttpHandlers(databaseSession, sessionStore)

    HttpRoutes.of[IO] {
      case request @ GET -> Root / "api" / "users" =>
        handlers.execute(
          request,
          UserListRequest(
            query = request.uri.query.params.get("q").flatMap(rawQuery => UserSearchQuery.parse(rawQuery).toOption),
            pageRequest = PageRequest(
              page = parsePage(request.uri.query.params.get("page")),
              pageSize = parsePageSize(request.uri.query.params.get("pageSize"))
            )
          ),
          listUsers
        )

      case request @ GET -> Root / "api" / "users" / "suggestions" =>
        UserSearchQuery.parse(request.uri.query.params.get("q").getOrElse("")) match
          case Left(message) => UserHttpResponses.validationErrorResponse(message)
          case Right(query) => handlers.execute(request, query, listUserSuggestions)

      case request @ GET -> Root / "api" / "users" / targetUsername / "profile" =>
        handlers.execute(request, Username.canonical(targetUsername), getUserProfile)

      case request @ GET -> Root / "api" / "users" / targetUsername / "settings" =>
        handlers.execute(request, Username.canonical(targetUsername), getUserSettings)

      case request @ GET -> Root / "api" / "users" / "ranklist" =>
        handlers.execute(request, PageRequest(page = parsePage(request.uri.query.params.get("page"))), listContributionRanklist)

      case request @ GET -> Root / "api" / "users" / "ranklist" / "accepted" =>
        handlers.execute(request, PageRequest(page = parsePage(request.uri.query.params.get("page"))), listAcceptedRanklist)

      case request @ POST -> Root / "api" / "users" / targetUsername / "permissions" =>
        handlers.executeDecoded[UpdateUserPermissionsRequest, (Username, UpdateUserPermissionsRequest), UserMutationCommands.UpdateUserPermissionsResult](
          request,
          updateUserPermissions
        )(body => (Username.canonical(targetUsername), body))

      case request @ POST -> Root / "api" / "users" / targetUsername / "settings" / "profile" =>
        handlers.executeUserSettingsProfileUpdate(request, Username.canonical(targetUsername))

      case request @ POST -> Root / "api" / "users" / targetUsername / "settings" / "preferences" =>
        handlers.executeUserSettingsPreferencesUpdate(request, Username.canonical(targetUsername))

      case request @ POST -> Root / "api" / "users" / targetUsername / "settings" / "account" =>
        handlers.executeUserSettingsAccountUpdate(request, Username.canonical(targetUsername))

      case request @ POST -> Root / "api" / "users" / targetUsername / "delete" =>
        handlers.execute(request, Username.canonical(targetUsername), deleteUser)
    }

  private def parsePage(rawPage: Option[String]): Int =
    rawPage.flatMap(_.toIntOption).getOrElse(1)

  private def parsePageSize(rawPageSize: Option[String]): Int =
    rawPageSize.flatMap(_.toIntOption).filter(_ > 0).getOrElse(10)
