package domains.usergroup.application

import cats.effect.IO
import database.DatabaseSession
import domains.auth.model.{AuthUser, Username}
import domains.auth.table.AuthUserTable
import domains.shared.model.{PageRequest, PageResponse}
import domains.usergroup.model.{AddUserGroupMemberRequest, CreateUserGroupRequest, ManagedUserGroup, OwnedUserGroup, UpdateUserGroupMemberRoleRequest, UpdateUserGroupRequest, UserGroup, UserGroupSlug, UserGroupRole, UserGroupSummary}
import domains.usergroup.table.UserGroupTable
import domains.usergroup.application.UserGroupDecisions.*

object UserGroupCommands:

  enum CreateUserGroupResult:
    case Forbidden
    case ValidationFailed(message: String)
    case SlugAlreadyExists
    case SlugConflictsWithUsername
    case Created(group: UserGroup)

  enum GetUserGroupResult:
    case NotFound
    case Forbidden
    case Found(group: UserGroup)

  enum UpdateUserGroupResult:
    case Forbidden
    case ValidationFailed(message: String)
    case NotFound
    case Updated(group: UserGroup)

  enum DeleteUserGroupResult:
    case Forbidden
    case NotFound
    case Deleted

  enum AddUserGroupMemberResult:
    case Forbidden
    case ValidationFailed(message: String)
    case UserGroupNotFound
    case UserNotFound
    case MemberAlreadyExists
    case Added(group: UserGroup)

  enum UpdateUserGroupMemberRoleResult:
    case Forbidden
    case ValidationFailed(message: String)
    case UserGroupNotFound
    case MemberNotFound
    case CannotModifyOwnerRole
    case OwnershipTransferRequired
    case Updated(group: UserGroup)

  enum RemoveUserGroupMemberResult:
    case Forbidden
    case UserGroupNotFound
    case MemberNotFound
    case CannotRemoveOwner
    case Removed(group: UserGroup)

  def listUserGroups(
    databaseSession: DatabaseSession,
    actor: AuthUser,
    pageRequest: PageRequest
  ): IO[PageResponse[UserGroupSummary]] =
    val normalizedPageRequest = pageRequest.normalized
    if !UserGroupPolicy.canList(actor) then
      IO.pure(PageResponse(items = Nil, page = normalizedPageRequest.page, pageSize = normalizedPageRequest.pageSize, totalItems = 0L))
    else
      databaseSession.withTransactionConnection { connection =>
        UserGroupTable.listVisibleTo(connection, actor, normalizedPageRequest.page, normalizedPageRequest.pageSize)
      }

  def createUserGroup(
    databaseSession: DatabaseSession,
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
          databaseSession.withTransactionConnection { connection =>
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
          }

  def getUserGroupBySlug(
    databaseSession: DatabaseSession,
    actor: AuthUser,
    slug: UserGroupSlug
  ): IO[GetUserGroupResult] =
    databaseSession.withTransactionConnection { connection =>
      UserGroupTable.findBySlug(connection, slug).map {
        case None => GetUserGroupResult.NotFound
        case Some(group) if !UserGroupPolicy.canView(actor, group) => GetUserGroupResult.Forbidden
        case Some(group) => GetUserGroupResult.Found(group)
      }
    }

  def updateUserGroup(
    databaseSession: DatabaseSession,
    actor: AuthUser,
    slug: UserGroupSlug,
    request: UpdateUserGroupRequest
  ): IO[UpdateUserGroupResult] =
    UserGroupValidation.validateUpdate(request) match
      case Left(message) =>
        IO.pure(UpdateUserGroupResult.ValidationFailed(message))
      case Right(validRequest) =>
        databaseSession.withTransactionConnection { connection =>
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
        }

  def deleteUserGroup(
    databaseSession: DatabaseSession,
    actor: AuthUser,
    slug: UserGroupSlug
  ): IO[DeleteUserGroupResult] =
    databaseSession.withTransactionConnection { connection =>
      for
        maybeGroup <- UserGroupTable.findBySlug(connection, slug)
        result <- maybeGroup match
          case None => IO.pure(DeleteUserGroupResult.NotFound)
          case Some(group) =>
            UserGroupPolicy.requireOwned(actor, group) match
              case None => IO.pure(DeleteUserGroupResult.Forbidden)
              case Some(ownedGroup) => deleteOwnedUserGroup(connection, ownedGroup)
      yield result
    }

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

  private def updateManagedUserGroup(
    connection: java.sql.Connection,
    managedGroup: ManagedUserGroup,
    request: UpdateUserGroupRequest
  ): IO[UpdateUserGroupResult] =
    UserGroupTable.update(connection, managedGroup.value.id, request).flatMap { _ =>
      UserGroupTable
        .findBySlug(connection, managedGroup.value.slug)
        .map(updatedUserGroupOrError("User group disappeared after update"))
        .map(UpdateUserGroupResult.Updated(_))
    }

  private def addMemberToManagedUserGroup(
    connection: java.sql.Connection,
    managedGroup: ManagedUserGroup,
    request: AddUserGroupMemberRequest
  ): IO[AddUserGroupMemberResult] =
    AuthUserTable.findByUsername(connection, request.username).flatMap {
      case None => IO.pure(AddUserGroupMemberResult.UserNotFound)
      case Some(targetUser) =>
        UserGroupTable.addMember(connection, managedGroup.value.id, request.copy(username = targetUser.username)).flatMap {
          case UserGroupTable.AddMemberTableResult.AlreadyExists =>
            IO.pure(AddUserGroupMemberResult.MemberAlreadyExists)
          case UserGroupTable.AddMemberTableResult.UserNotFound =>
            IO.pure(AddUserGroupMemberResult.UserNotFound)
          case UserGroupTable.AddMemberTableResult.Added =>
            UserGroupTable
              .findBySlug(connection, managedGroup.value.slug)
              .map(updatedUserGroupOrError("User group disappeared after member add"))
              .map(AddUserGroupMemberResult.Added(_))
        }
    }

  private def deleteOwnedUserGroup(
    connection: java.sql.Connection,
    ownedGroup: OwnedUserGroup
  ): IO[DeleteUserGroupResult] =
    UserGroupTable.delete(connection, ownedGroup.value.id).as(DeleteUserGroupResult.Deleted)

  private def updateMemberRoleForOwnedGroup(
    connection: java.sql.Connection,
    ownedGroup: OwnedUserGroup,
    targetUsername: Username,
    request: UpdateUserGroupMemberRoleRequest
  ): IO[UpdateUserGroupMemberRoleResult] =
    decideMemberRoleUpdate(ownedGroup.value, targetUsername, request) match
      case MemberRoleUpdateDecision.MemberNotFound =>
        IO.pure(UpdateUserGroupMemberRoleResult.MemberNotFound)
      case MemberRoleUpdateDecision.CannotModifyOwnerRole =>
        IO.pure(UpdateUserGroupMemberRoleResult.CannotModifyOwnerRole)
      case MemberRoleUpdateDecision.TransferOwnership(canonicalTargetUsername) =>
        UserGroupTable.transferOwnership(
          connection,
          ownedGroup.value.id,
          ownedGroup.value.ownerUsername,
          canonicalTargetUsername,
        ).flatMap {
          case UserGroupTable.UpdateMemberRoleTableResult.MemberNotFound =>
            IO.pure(UpdateUserGroupMemberRoleResult.MemberNotFound)
          case UserGroupTable.UpdateMemberRoleTableResult.Updated =>
            UserGroupTable
              .findBySlug(connection, ownedGroup.value.slug)
              .map(updatedUserGroupOrError("User group disappeared after ownership transfer"))
              .map(UpdateUserGroupMemberRoleResult.Updated(_))
        }
      case MemberRoleUpdateDecision.UpdateRole(canonicalTargetUsername, role) =>
        UserGroupTable.updateMemberRole(connection, ownedGroup.value.id, canonicalTargetUsername, role).flatMap {
          case UserGroupTable.UpdateMemberRoleTableResult.MemberNotFound =>
            IO.pure(UpdateUserGroupMemberRoleResult.MemberNotFound)
          case UserGroupTable.UpdateMemberRoleTableResult.Updated =>
            UserGroupTable
              .findBySlug(connection, ownedGroup.value.slug)
              .map(updatedUserGroupOrError("User group disappeared after member role update"))
              .map(UpdateUserGroupMemberRoleResult.Updated(_))
        }

  private def removeMemberFromGroup(
    connection: java.sql.Connection,
    actor: AuthUser,
    group: UserGroup,
    targetUsername: Username
  ): IO[RemoveUserGroupMemberResult] =
    decideMemberRemoval(actor, group, targetUsername) match
      case MemberRemovalDecision.MemberNotFound =>
        IO.pure(RemoveUserGroupMemberResult.MemberNotFound)
      case MemberRemovalDecision.CannotRemoveOwner =>
        IO.pure(RemoveUserGroupMemberResult.CannotRemoveOwner)
      case MemberRemovalDecision.Forbidden =>
        IO.pure(RemoveUserGroupMemberResult.Forbidden)
      case MemberRemovalDecision.Remove(canonicalTargetUsername) =>
        UserGroupTable.removeMember(connection, group.id, canonicalTargetUsername).flatMap {
          case UserGroupTable.RemoveMemberTableResult.MemberNotFound =>
            IO.pure(RemoveUserGroupMemberResult.MemberNotFound)
          case UserGroupTable.RemoveMemberTableResult.Removed =>
            UserGroupTable
              .findBySlug(connection, group.slug)
              .map(updatedUserGroupOrError("User group disappeared after member removal"))
              .map(RemoveUserGroupMemberResult.Removed(_))
        }

  private def updatedUserGroupOrError(message: String)(maybeGroup: Option[UserGroup]): UserGroup =
    maybeGroup.getOrElse(throw new IllegalStateException(message))
