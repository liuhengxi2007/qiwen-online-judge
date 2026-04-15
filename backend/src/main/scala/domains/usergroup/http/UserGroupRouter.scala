package domains.usergroup.http

import cats.effect.IO
import database.DatabaseSession
import domains.auth.application.SessionStore
import domains.auth.http.AuthHttpSessionSupport
import domains.shared.model.PageRequest
import domains.usergroup.application.UserGroupCommands
import domains.auth.model.Username
import domains.usergroup.model.{AddUserGroupMemberRequest, CreateUserGroupRequest, UpdateUserGroupMemberRoleRequest, UpdateUserGroupRequest, UserGroupSlug}
import io.circe.syntax.*
import org.http4s.HttpRoutes
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.circe.CirceEntityEncoder.*
import org.http4s.dsl.Http4sDsl
import org.http4s.dsl.io.*

object UserGroupRouter:

  def routes(databaseSession: DatabaseSession, sessionStore: SessionStore): HttpRoutes[IO] =
    given Http4sDsl[IO] = new Http4sDsl[IO] {}
    val handlers = new UserGroupHttpHandlers(databaseSession, sessionStore)
    HttpRoutes.of[IO] {
      case request @ GET -> Root / "api" / "user-groups" =>
        handlers.listUserGroups(request)

      case request @ GET -> Root / "api" / "user-groups" / groupSlug =>
        UserGroupSlug.parse(groupSlug) match
          case Left(message) =>
            UserGroupHttpResponses.validationErrorResponse(message)
          case Right(parsedGroupSlug) =>
            handlers.getUserGroup(request, parsedGroupSlug)

      case request @ POST -> Root / "api" / "user-groups" =>
        handlers.createUserGroup(request)

      case request @ POST -> Root / "api" / "user-groups" / groupSlug =>
        UserGroupSlug.parse(groupSlug) match
          case Left(message) =>
            UserGroupHttpResponses.validationErrorResponse(message)
          case Right(parsedGroupSlug) =>
            handlers.updateUserGroup(request, parsedGroupSlug)

      case request @ POST -> Root / "api" / "user-groups" / groupSlug / "delete" =>
        UserGroupSlug.parse(groupSlug) match
          case Left(message) =>
            UserGroupHttpResponses.validationErrorResponse(message)
          case Right(parsedGroupSlug) =>
            handlers.deleteUserGroup(request, parsedGroupSlug)

      case request @ POST -> Root / "api" / "user-groups" / groupSlug / "members" =>
        UserGroupSlug.parse(groupSlug) match
          case Left(message) =>
            UserGroupHttpResponses.validationErrorResponse(message)
          case Right(parsedGroupSlug) =>
            handlers.addMember(request, parsedGroupSlug)

      case request @ POST -> Root / "api" / "user-groups" / groupSlug / "members" / memberUsername / "role" =>
        UserGroupSlug.parse(groupSlug) match
          case Left(message) =>
            UserGroupHttpResponses.validationErrorResponse(message)
          case Right(parsedGroupSlug) =>
            handlers.updateMemberRole(request, parsedGroupSlug, Username.canonical(memberUsername))

      case request @ POST -> Root / "api" / "user-groups" / groupSlug / "members" / memberUsername / "remove" =>
        UserGroupSlug.parse(groupSlug) match
          case Left(message) =>
            UserGroupHttpResponses.validationErrorResponse(message)
          case Right(parsedGroupSlug) =>
            handlers.removeMember(request, parsedGroupSlug, Username.canonical(memberUsername))
    }
