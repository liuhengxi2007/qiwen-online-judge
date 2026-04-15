package domains.usergroup.http

import cats.effect.IO
import database.DatabaseSession
import domains.auth.application.SessionStore
import domains.auth.http.AuthHttpSessionSupport
import domains.auth.model.Username
import domains.shared.model.PageRequest
import domains.usergroup.application.UserGroupCommands
import domains.usergroup.model.{AddUserGroupMemberRequest, CreateUserGroupRequest, UpdateUserGroupMemberRoleRequest, UpdateUserGroupRequest, UserGroupSlug}
import io.circe.syntax.*
import org.http4s.{Request, Response}
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.dsl.Http4sDsl

final class UserGroupHttpHandlers(
  databaseSession: DatabaseSession,
  sessionStore: SessionStore
)(using dsl: Http4sDsl[IO]):

  import dsl.*

  def listUserGroups(request: Request[IO]): IO[Response[IO]] =
    AuthHttpSessionSupport.withAuthenticatedUser(databaseSession, sessionStore, request) { actor =>
      UserGroupCommands
        .listUserGroups(databaseSession, actor, PageRequest())
        .flatMap(response => Ok(response.asJson))
    }

  def getUserGroup(request: Request[IO], parsedGroupSlug: UserGroupSlug): IO[Response[IO]] =
    AuthHttpSessionSupport.withAuthenticatedUser(databaseSession, sessionStore, request) { actor =>
      UserGroupCommands
        .getUserGroupBySlug(databaseSession, actor, parsedGroupSlug)
        .flatMap(UserGroupHttpResponses.mapGetResult)
    }

  def createUserGroup(request: Request[IO]): IO[Response[IO]] =
    AuthHttpSessionSupport.withAuthenticatedUser(databaseSession, sessionStore, request) { actor =>
      for
        createRequest <- request.as[CreateUserGroupRequest]
        response <- UserGroupCommands
          .createUserGroup(databaseSession, actor, createRequest)
          .flatMap(UserGroupHttpResponses.mapCreateResult)
      yield response
    }

  def updateUserGroup(request: Request[IO], parsedGroupSlug: UserGroupSlug): IO[Response[IO]] =
    AuthHttpSessionSupport.withAuthenticatedUser(databaseSession, sessionStore, request) { actor =>
      for
        updateRequest <- request.as[UpdateUserGroupRequest]
        response <- UserGroupCommands
          .updateUserGroup(databaseSession, actor, parsedGroupSlug, updateRequest)
          .flatMap(UserGroupHttpResponses.mapUpdateResult)
      yield response
    }

  def deleteUserGroup(request: Request[IO], parsedGroupSlug: UserGroupSlug): IO[Response[IO]] =
    AuthHttpSessionSupport.withAuthenticatedUser(databaseSession, sessionStore, request) { actor =>
      UserGroupCommands
        .deleteUserGroup(databaseSession, actor, parsedGroupSlug)
        .flatMap(UserGroupHttpResponses.mapDeleteResult)
    }

  def addMember(request: Request[IO], parsedGroupSlug: UserGroupSlug): IO[Response[IO]] =
    AuthHttpSessionSupport.withAuthenticatedUser(databaseSession, sessionStore, request) { actor =>
      for
        addMemberRequest <- request.as[AddUserGroupMemberRequest]
        response <- UserGroupCommands
          .addUserGroupMember(databaseSession, actor, parsedGroupSlug, addMemberRequest)
          .flatMap(UserGroupHttpResponses.mapAddMemberResult)
      yield response
    }

  def updateMemberRole(
    request: Request[IO],
    parsedGroupSlug: UserGroupSlug,
    memberUsername: Username
  ): IO[Response[IO]] =
    AuthHttpSessionSupport.withAuthenticatedUser(databaseSession, sessionStore, request) { actor =>
      for
        updateRoleRequest <- request.as[UpdateUserGroupMemberRoleRequest]
        response <- UserGroupCommands
          .updateUserGroupMemberRole(databaseSession, actor, parsedGroupSlug, memberUsername, updateRoleRequest)
          .flatMap(UserGroupHttpResponses.mapUpdateMemberRoleResult)
      yield response
    }

  def removeMember(
    request: Request[IO],
    parsedGroupSlug: UserGroupSlug,
    memberUsername: Username
  ): IO[Response[IO]] =
    AuthHttpSessionSupport.withAuthenticatedUser(databaseSession, sessionStore, request) { actor =>
      UserGroupCommands
        .removeUserGroupMember(databaseSession, actor, parsedGroupSlug, memberUsername)
        .flatMap(UserGroupHttpResponses.mapRemoveMemberResult)
    }
