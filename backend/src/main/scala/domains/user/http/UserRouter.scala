package domains.user.http

import cats.effect.IO
import database.DatabaseSession
import domains.auth.application.SessionStore
import domains.auth.http.AuthHttpSessionSupport
import domains.auth.model.Username
import domains.shared.model.PageRequest
import domains.user.application.{UserMutationCommands, UserQueryCommands}
import domains.user.model.{UpdateManagedUserSettingsRequest, UpdateOwnSettingsRequest, UpdateUserPermissionsRequest}
import io.circe.Json
import org.http4s.HttpRoutes
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.dsl.Http4sDsl
import org.http4s.dsl.io.*

object UserRouter:

  def routes(databaseSession: DatabaseSession, sessionStore: SessionStore): HttpRoutes[IO] =
    given Http4sDsl[IO] = new Http4sDsl[IO] {}

    HttpRoutes.of[IO] {
      case request @ GET -> Root / "api" / "users" =>
        AuthHttpSessionSupport.withSiteManager(databaseSession, sessionStore, request) { actor =>
          UserMutationCommands
            .listUsers(databaseSession, actor)
            .flatMap(UserHttpResponses.listUsersResponse)
        }

      case request @ GET -> Root / "api" / "users" / targetUsername / "profile" =>
        AuthHttpSessionSupport.withAuthenticatedUser(databaseSession, sessionStore, request) { actor =>
          UserQueryCommands
            .getUserProfile(databaseSession, actor, Username.canonical(targetUsername))
            .flatMap(UserHttpResponses.mapGetUserProfileResult)
        }

      case request @ GET -> Root / "api" / "users" / targetUsername / "settings" =>
        AuthHttpSessionSupport.withAuthenticatedUser(databaseSession, sessionStore, request) { actor =>
          UserMutationCommands
            .getUserSettings(databaseSession, actor, Username.canonical(targetUsername))
            .flatMap(UserHttpResponses.mapGetUserSettingsResult)
        }

      case request @ GET -> Root / "api" / "users" / "ranklist" =>
        AuthHttpSessionSupport.withAuthenticatedUser(databaseSession, sessionStore, request) { actor =>
          UserQueryCommands
            .listContributionRanklist(databaseSession, actor, PageRequest(page = parsePage(request.uri.query.params.get("page"))))
            .flatMap(UserHttpResponses.listContributionRanklistResponse)
        }

      case request @ GET -> Root / "api" / "users" / "ranklist" / "accepted" =>
        AuthHttpSessionSupport.withAuthenticatedUser(databaseSession, sessionStore, request) { actor =>
          UserQueryCommands
            .listAcceptedRanklist(databaseSession, actor, PageRequest(page = parsePage(request.uri.query.params.get("page"))))
            .flatMap(UserHttpResponses.listAcceptedRanklistResponse)
        }

      case request @ POST -> Root / "api" / "users" / targetUsername / "permissions" =>
        AuthHttpSessionSupport.withSiteManager(databaseSession, sessionStore, request) { actor =>
          request.as[UpdateUserPermissionsRequest].flatMap { body =>
            databaseSession.withTransactionConnection(connection =>
              UserMutationCommands
                .updateUserPermissions(connection, actor.authUser, Username.canonical(targetUsername), body)
                .flatMap(UserHttpResponses.mapUpdateUserPermissionsResult)
            )
          }
        }

      case request @ POST -> Root / "api" / "users" / targetUsername / "settings" =>
        AuthHttpSessionSupport.withAuthenticatedUser(databaseSession, sessionStore, request) { actor =>
          request.as[Json].flatMap { json =>
            val target = Username.canonical(targetUsername)
            val commandResult: Either[String, UserMutationCommands.UpdateUserSettingsCommand] =
              if target.value == actor.username.value then
                json
                  .as[UpdateOwnSettingsRequest]
                  .left
                  .map(_.getMessage)
                  .map(request => UserMutationCommands.UpdateUserSettingsCommand.UpdateOwn(actor, request))
              else
                json
                  .as[UpdateManagedUserSettingsRequest]
                  .left
                  .map(_.getMessage)
                  .flatMap(request =>
                    domains.auth.model.SiteManagerUser
                      .from(actor)
                      .toRight("Site manager permission required.")
                      .map(siteManagerActor => UserMutationCommands.UpdateUserSettingsCommand.UpdateManaged(siteManagerActor, request))
                  )

            commandResult match
              case Left(message) =>
                UserHttpResponses.validationErrorResponse(message)
              case Right(command) =>
                databaseSession.withTransactionConnection { connection =>
                  for
                    result <- UserMutationCommands.updateUserSettings(connection, target, command)
                    _ <- revokePasswordChangedSessions(sessionStore, target, result)
                    response <- UserHttpResponses.mapUpdateUserSettingsResult(result)
                  yield
                    if passwordChangedByActor(actor, target, result) then response.addCookie(domains.auth.http.AuthHttpResponses.clearedSessionCookie)
                    else response
                }
          }
        }

      case request @ POST -> Root / "api" / "users" / targetUsername / "delete" =>
        AuthHttpSessionSupport.withSiteManager(databaseSession, sessionStore, request) { actor =>
          databaseSession.withTransactionConnection(connection =>
            UserMutationCommands
              .deleteUser(connection, actor.authUser, Username.canonical(targetUsername))
              .flatMap(UserHttpResponses.mapDeleteUserResult)
          )
        }
    }

  private def parsePage(rawPage: Option[String]): Int =
    rawPage.flatMap(_.toIntOption).getOrElse(1)

  private def revokePasswordChangedSessions(
    sessionStore: SessionStore,
    targetUsername: Username,
    result: UserMutationCommands.UpdateUserSettingsResult
  ): IO[Unit] =
    result match
      case UserMutationCommands.UpdateUserSettingsResult.Updated(_, true) =>
        sessionStore.deleteSessionsForUsername(targetUsername)
      case _ =>
        IO.unit

  private def passwordChangedByActor(
    actor: domains.auth.model.AuthUser,
    targetUsername: Username,
    result: UserMutationCommands.UpdateUserSettingsResult
  ): Boolean =
    result match
      case UserMutationCommands.UpdateUserSettingsResult.Updated(_, true) =>
        actor.username.value == targetUsername.value
      case _ =>
        false
