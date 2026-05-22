package domains.usergroup.application



import cats.effect.IO
import database.DatabaseSession
import domains.auth.model.{AuthUser, Username}
import domains.auth.table.AuthUserTable
import domains.usergroup.http.request.{CreateUserGroupRequest, UpdateUserGroupRequest}
import domains.usergroup.model.{UserGroupSlug}
import domains.usergroup.table.UserGroupTable
import domains.usergroup.application.UserGroupCommandResults.*
import domains.usergroup.application.UserGroupDecisions.*
import domains.usergroup.application.utils.UserGroupCommandSupport.*

import java.sql.Connection

object UserGroupMutationCommands:

  def createUserGroup(
    databaseSession: DatabaseSession,
    actor: AuthUser,
    request: CreateUserGroupRequest
  ): IO[CreateUserGroupResult] =
    databaseSession.withTransactionConnection(connection =>
      createUserGroup(connection, actor, request)
    )

  def createUserGroup(
    connection: Connection,
    actor: AuthUser,
    request: CreateUserGroupRequest
  ): IO[CreateUserGroupResult] =
    if !UserGroupPolicy.canCreate(actor) then
      IO.pure(CreateUserGroupResult.Forbidden)
    else
      UserGroupValidation.validateCreate(request) match
        case Left(message) =>
          IO.pure(CreateUserGroupResult.ValidationFailed(message))
        case Right(validRequest) =>
          for
            existing <- UserGroupTable.findBySlug(connection, validRequest.slug)
            conflictingUser <- AuthUserTable.findByUsername(connection, Username.canonical(validRequest.slug.value))
            result <- decideCreateUserGroup(existing, conflictingUser) match
              case CreateUserGroupDecision.SlugAlreadyExists =>
                IO.pure(CreateUserGroupResult.SlugAlreadyExists)
              case CreateUserGroupDecision.SlugConflictsWithUsername =>
                IO.pure(CreateUserGroupResult.SlugConflictsWithUsername)
              case CreateUserGroupDecision.Create =>
                UserGroupTable.insert(connection, actor.username, validRequest).map(CreateUserGroupResult.Created(_))
          yield result

  def updateUserGroup(
    databaseSession: DatabaseSession,
    actor: AuthUser,
    slug: UserGroupSlug,
    request: UpdateUserGroupRequest
  ): IO[UpdateUserGroupResult] =
    databaseSession.withTransactionConnection(connection =>
      updateUserGroup(connection, actor, slug, request)
    )

  def updateUserGroup(
    connection: Connection,
    actor: AuthUser,
    slug: UserGroupSlug,
    request: UpdateUserGroupRequest
  ): IO[UpdateUserGroupResult] =
    UserGroupValidation.validateUpdate(request) match
      case Left(message) =>
        IO.pure(UpdateUserGroupResult.ValidationFailed(message))
      case Right(validRequest) =>
        for
          maybeGroup <- UserGroupTable.findBySlug(connection, slug)
          result <- decideUpdateUserGroup(actor, maybeGroup) match
            case UpdateUserGroupDecision.NotFound =>
              IO.pure(UpdateUserGroupResult.NotFound)
            case UpdateUserGroupDecision.Forbidden =>
              IO.pure(UpdateUserGroupResult.Forbidden)
            case UpdateUserGroupDecision.Update(managedGroup) =>
              updateManagedUserGroup(connection, managedGroup, validRequest)
        yield result

  def deleteUserGroup(
    databaseSession: DatabaseSession,
    actor: AuthUser,
    slug: UserGroupSlug
  ): IO[DeleteUserGroupResult] =
    databaseSession.withTransactionConnection(connection =>
      deleteUserGroup(connection, actor, slug)
    )

  def deleteUserGroup(
    connection: Connection,
    actor: AuthUser,
    slug: UserGroupSlug
  ): IO[DeleteUserGroupResult] =
    for
      maybeGroup <- UserGroupTable.findBySlug(connection, slug)
      result <- maybeGroup match
        case None => IO.pure(DeleteUserGroupResult.NotFound)
        case Some(group) =>
          UserGroupPolicy.requireOwned(actor, group) match
            case None => IO.pure(DeleteUserGroupResult.Forbidden)
            case Some(ownedGroup) => deleteOwnedUserGroup(connection, ownedGroup)
    yield result
