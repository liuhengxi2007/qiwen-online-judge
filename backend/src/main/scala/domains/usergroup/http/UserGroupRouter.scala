package domains.usergroup.http

import cats.effect.IO
import database.DatabaseSession
import domains.auth.application.SessionStore
import domains.usergroup.application.UserGroupCommands
import domains.auth.model.Username
import domains.shared.http.AuthenticatedHttpExecutor
import domains.usergroup.model.{AddUserGroupMemberRequest, CreateUserGroupRequest, UpdateUserGroupMemberRoleRequest, UpdateUserGroupRequest, UserGroupSlug}
import org.http4s.HttpRoutes
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.dsl.Http4sDsl
import org.http4s.dsl.io.*

object UserGroupRouter:

  def routes(databaseSession: DatabaseSession, sessionStore: SessionStore): HttpRoutes[IO] =
    given Http4sDsl[IO] = new Http4sDsl[IO] {}
    val handlers = new AuthenticatedHttpExecutor(databaseSession, sessionStore)
    HttpRoutes.of[IO] {
      case request @ GET -> Root / "api" / "user-groups" =>
        handlers.execute(request, parsePageRequest(request.uri.query.params), UserGroupHttpPlanDefinitions.listUserGroups)

      case request @ GET -> Root / "api" / "user-groups" / groupSlug =>
        UserGroupSlug.parse(groupSlug) match
          case Left(message) =>
            UserGroupHttpResponses.validationErrorResponse(message)
          case Right(parsedGroupSlug) =>
            handlers.execute(request, parsedGroupSlug, UserGroupHttpPlanDefinitions.getUserGroup)

      case request @ POST -> Root / "api" / "user-groups" =>
        handlers.executeDecoded[CreateUserGroupRequest, CreateUserGroupRequest, UserGroupCommands.CreateUserGroupResult](
          request,
          UserGroupHttpPlanDefinitions.createUserGroup
        )(identity)

      case request @ POST -> Root / "api" / "user-groups" / groupSlug =>
        UserGroupSlug.parse(groupSlug) match
          case Left(message) =>
            UserGroupHttpResponses.validationErrorResponse(message)
          case Right(parsedGroupSlug) =>
            handlers.executeDecoded[
              UpdateUserGroupRequest,
              (UserGroupSlug, UpdateUserGroupRequest),
              UserGroupCommands.UpdateUserGroupResult
            ](
              request,
              UserGroupHttpPlanDefinitions.updateUserGroup
            )(updateRequest => (parsedGroupSlug, updateRequest))

      case request @ POST -> Root / "api" / "user-groups" / groupSlug / "delete" =>
        UserGroupSlug.parse(groupSlug) match
          case Left(message) =>
            UserGroupHttpResponses.validationErrorResponse(message)
          case Right(parsedGroupSlug) =>
            handlers.execute(request, parsedGroupSlug, UserGroupHttpPlanDefinitions.deleteUserGroup)

      case request @ POST -> Root / "api" / "user-groups" / groupSlug / "members" =>
        UserGroupSlug.parse(groupSlug) match
          case Left(message) =>
            UserGroupHttpResponses.validationErrorResponse(message)
          case Right(parsedGroupSlug) =>
            handlers.executeDecoded[
              AddUserGroupMemberRequest,
              (UserGroupSlug, AddUserGroupMemberRequest),
              UserGroupCommands.AddUserGroupMemberResult
            ](
              request,
              UserGroupHttpPlanDefinitions.addMember
            )(addMemberRequest => (parsedGroupSlug, addMemberRequest))

      case request @ POST -> Root / "api" / "user-groups" / groupSlug / "members" / memberUsername / "role" =>
        UserGroupSlug.parse(groupSlug) match
          case Left(message) =>
            UserGroupHttpResponses.validationErrorResponse(message)
          case Right(parsedGroupSlug) =>
            handlers.executeDecoded[
              UpdateUserGroupMemberRoleRequest,
              (UserGroupSlug, Username, UpdateUserGroupMemberRoleRequest),
              UserGroupCommands.UpdateUserGroupMemberRoleResult
            ](
              request,
              UserGroupHttpPlanDefinitions.updateMemberRole
            )(updateRoleRequest => (parsedGroupSlug, Username.canonical(memberUsername), updateRoleRequest))

      case request @ POST -> Root / "api" / "user-groups" / groupSlug / "members" / memberUsername / "remove" =>
        UserGroupSlug.parse(groupSlug) match
          case Left(message) =>
            UserGroupHttpResponses.validationErrorResponse(message)
          case Right(parsedGroupSlug) =>
            handlers.execute(
              request,
              (parsedGroupSlug, Username.canonical(memberUsername)),
              UserGroupHttpPlanDefinitions.removeMember
            )
    }

  private def parsePageRequest(queryParams: Map[String, String]): domains.shared.model.PageRequest =
    domains.shared.model.PageRequest(
      page = parsePositiveInt(queryParams.get("page"), 1),
      pageSize = parsePositiveInt(queryParams.get("pageSize"), 10)
    )

  private def parsePositiveInt(rawValue: Option[String], defaultValue: Int): Int =
    rawValue.flatMap(_.toIntOption).filter(_ > 0).getOrElse(defaultValue)
