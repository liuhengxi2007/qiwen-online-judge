package domains.usergroup.application

import cats.effect.IO
import database.DatabaseSession
import domains.auth.model.{AuthUser, Username}
import domains.usergroup.model.{AddUserGroupMemberRequest, UpdateUserGroupMemberRoleRequest, UserGroupSlug}
import domains.usergroup.table.UserGroupTable
import domains.usergroup.application.UserGroupCommandResults.*
import domains.usergroup.application.UserGroupCommandSupport.*

object UserGroupMemberCommands:

  def addUserGroupMember(
    databaseSession: DatabaseSession,
    actor: AuthUser,
    slug: UserGroupSlug,
    request: AddUserGroupMemberRequest
  ): IO[AddUserGroupMemberResult] =
    UserGroupValidation.validateAddMember(request) match
      case Left(message) =>
        IO.pure(AddUserGroupMemberResult.ValidationFailed(message))
      case Right(validRequest) =>
        databaseSession.withTransactionConnection { connection =>
          for
            maybeGroup <- UserGroupTable.findBySlug(connection, slug)
            result <- maybeGroup match
              case None => IO.pure(AddUserGroupMemberResult.UserGroupNotFound)
              case Some(group) =>
                UserGroupPolicy.requireManaged(actor, group) match
                  case None =>
                    IO.pure(AddUserGroupMemberResult.Forbidden)
                  case Some(managedGroup) =>
                    addMemberToManagedUserGroup(connection, managedGroup, validRequest)
          yield result
        }

  def updateUserGroupMemberRole(
    databaseSession: DatabaseSession,
    actor: AuthUser,
    slug: UserGroupSlug,
    targetUsername: Username,
    request: UpdateUserGroupMemberRoleRequest
  ): IO[UpdateUserGroupMemberRoleResult] =
    UserGroupValidation.validateUpdateMemberRole(request) match
      case Left(message) =>
        IO.pure(UpdateUserGroupMemberRoleResult.ValidationFailed(message))
      case Right(validRequest) =>
        databaseSession.withTransactionConnection { connection =>
          for
            maybeGroup <- UserGroupTable.findBySlug(connection, slug)
            result <- maybeGroup match
              case None =>
                IO.pure(UpdateUserGroupMemberRoleResult.UserGroupNotFound)
              case Some(group) =>
                UserGroupPolicy.requireOwned(actor, group) match
                  case None =>
                    IO.pure(UpdateUserGroupMemberRoleResult.Forbidden)
                  case Some(ownedGroup) =>
                    updateMemberRoleForOwnedGroup(connection, ownedGroup, targetUsername, validRequest)
          yield result
        }

  def removeUserGroupMember(
    databaseSession: DatabaseSession,
    actor: AuthUser,
    slug: UserGroupSlug,
    targetUsername: Username
  ): IO[RemoveUserGroupMemberResult] =
    databaseSession.withTransactionConnection { connection =>
      for
        maybeGroup <- UserGroupTable.findBySlug(connection, slug)
        result <- maybeGroup match
          case None =>
            IO.pure(RemoveUserGroupMemberResult.UserGroupNotFound)
          case Some(group) =>
            removeMemberFromGroup(connection, actor, group, targetUsername)
      yield result
    }
