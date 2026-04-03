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
import org.http4s.dsl.io.*

object UserGroupRouter:

  def routes(databaseSession: DatabaseSession, sessionStore: SessionStore): HttpRoutes[IO] =
    val sessionSupport = new AuthHttpSessionSupport(databaseSession, sessionStore)

    HttpRoutes.of[IO] {
      case request @ GET -> Root / "api" / "user-groups" =>
        sessionSupport.withAuthenticatedUser(request) { actor =>
          UserGroupCommands
            .listUserGroups(databaseSession, actor, PageRequest())
            .flatMap(response => Ok(UserGroupHttpResponses.toUserGroupListResponse(response).asJson))
        }

      case request @ GET -> Root / "api" / "user-groups" / groupSlug =>
        sessionSupport.withAuthenticatedUser(request) { actor =>
          UserGroupCommands
            .getUserGroupBySlug(databaseSession, actor, UserGroupSlug(groupSlug))
            .flatMap(UserGroupHttpResponses.mapGetResult)
        }

      case request @ POST -> Root / "api" / "user-groups" =>
        sessionSupport.withAuthenticatedUser(request) { actor =>
          for
            createRequest <- request.as[CreateUserGroupRequest]
            response <- UserGroupCommands
              .createUserGroup(databaseSession, actor, createRequest)
              .flatMap(UserGroupHttpResponses.mapCreateResult)
          yield response
        }

      case request @ POST -> Root / "api" / "user-groups" / groupSlug =>
        sessionSupport.withAuthenticatedUser(request) { actor =>
          for
            updateRequest <- request.as[UpdateUserGroupRequest]
            response <- UserGroupCommands
              .updateUserGroup(databaseSession, actor, UserGroupSlug(groupSlug), updateRequest)
              .flatMap(UserGroupHttpResponses.mapUpdateResult)
          yield response
        }

      case request @ POST -> Root / "api" / "user-groups" / groupSlug / "delete" =>
        sessionSupport.withAuthenticatedUser(request) { actor =>
          UserGroupCommands
            .deleteUserGroup(databaseSession, actor, UserGroupSlug(groupSlug))
            .flatMap(UserGroupHttpResponses.mapDeleteResult)
        }

      case request @ POST -> Root / "api" / "user-groups" / groupSlug / "members" =>
        sessionSupport.withAuthenticatedUser(request) { actor =>
          for
            addMemberRequest <- request.as[AddUserGroupMemberRequest]
            response <- UserGroupCommands
              .addUserGroupMember(databaseSession, actor, UserGroupSlug(groupSlug), addMemberRequest)
              .flatMap(UserGroupHttpResponses.mapAddMemberResult)
          yield response
        }

      case request @ POST -> Root / "api" / "user-groups" / groupSlug / "members" / memberUsername / "role" =>
        sessionSupport.withAuthenticatedUser(request) { actor =>
          for
            updateRoleRequest <- request.as[UpdateUserGroupMemberRoleRequest]
            response <- UserGroupCommands
              .updateUserGroupMemberRole(databaseSession, actor, UserGroupSlug(groupSlug), Username(memberUsername), updateRoleRequest)
              .flatMap(UserGroupHttpResponses.mapUpdateMemberRoleResult)
          yield response
        }
    }
