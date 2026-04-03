package domains.usergroup.application

import cats.effect.IO
import database.DatabaseSession
import domains.auth.model.{AuthUser, Username}
import domains.shared.model.{PageRequest, PageResponse}
import domains.usergroup.model.{AddUserGroupMemberRequest, CreateUserGroupRequest, ManagedUserGroup, OwnedUserGroup, UpdateUserGroupMemberRoleRequest, UpdateUserGroupRequest, UserGroupDetail, UserGroupListResponse, UserGroupSlug, UserGroupRole}
import domains.usergroup.table.UserGroupTable

object UserGroupCommands:

  enum CreateUserGroupResult:
    case Forbidden
    case ValidationFailed(message: String)
    case SlugAlreadyExists
    case Created(group: UserGroupDetail)

  enum GetUserGroupResult:
    case NotFound
    case Forbidden
    case Found(group: UserGroupDetail)

  enum UpdateUserGroupResult:
    case Forbidden
    case ValidationFailed(message: String)
    case NotFound
    case Updated(group: UserGroupDetail)

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
    case Added(group: UserGroupDetail)

  enum UpdateUserGroupMemberRoleResult:
    case Forbidden
    case ValidationFailed(message: String)
    case UserGroupNotFound
    case MemberNotFound
    case CannotModifyOwnerRole
    case OwnershipTransferRequired
    case Updated(group: UserGroupDetail)

  def listUserGroups(
    databaseSession: DatabaseSession,
    actor: AuthUser,
    pageRequest: PageRequest
  ): IO[UserGroupListResponse] =
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
              result <- existing match
                case Some(_) => IO.pure(CreateUserGroupResult.SlugAlreadyExists)
                case None => UserGroupTable.insert(connection, actor.username, validRequest).map(CreateUserGroupResult.Created(_))
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
            result <- maybeGroup match
              case None => IO.pure(UpdateUserGroupResult.NotFound)
              case Some(group) =>
                UserGroupPolicy.requireManaged(actor, group) match
                  case None => IO.pure(UpdateUserGroupResult.Forbidden)
                  case Some(managedGroup) => updateManagedUserGroup(connection, managedGroup, validRequest)
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

  private def updateManagedUserGroup(
    connection: java.sql.Connection,
    managedGroup: ManagedUserGroup,
    request: UpdateUserGroupRequest
  ): IO[UpdateUserGroupResult] =
    UserGroupTable.update(connection, managedGroup.value.id, request).flatMap { _ =>
      UserGroupTable.findBySlug(connection, managedGroup.value.slug).map {
        case Some(updatedGroup) => UpdateUserGroupResult.Updated(updatedGroup)
        case None => throw new IllegalStateException("User group disappeared after update")
      }
    }

  private def addMemberToManagedUserGroup(
    connection: java.sql.Connection,
    managedGroup: ManagedUserGroup,
    request: AddUserGroupMemberRequest
  ): IO[AddUserGroupMemberResult] =
    UserGroupTable.userExists(connection, request.username).flatMap {
      case false => IO.pure(AddUserGroupMemberResult.UserNotFound)
      case true =>
        UserGroupTable.addMember(connection, managedGroup.value.id, request).flatMap {
          case UserGroupTable.AddMemberTableResult.AlreadyExists =>
            IO.pure(AddUserGroupMemberResult.MemberAlreadyExists)
          case UserGroupTable.AddMemberTableResult.Added =>
            UserGroupTable.findBySlug(connection, managedGroup.value.slug).map {
              case Some(updatedGroup) => AddUserGroupMemberResult.Added(updatedGroup)
              case None => throw new IllegalStateException("User group disappeared after member add")
            }
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
    val currentOwnerUsername = ownedGroup.value.ownerUsername
    val targetMembership = ownedGroup.value.members.find(_.username.value.equalsIgnoreCase(targetUsername.value))

    targetMembership match
      case None =>
        IO.pure(UpdateUserGroupMemberRoleResult.MemberNotFound)
      case Some(targetMember) =>
        if targetMember.role == UserGroupRole.Owner && request.role != UserGroupRole.Owner then
          IO.pure(UpdateUserGroupMemberRoleResult.CannotModifyOwnerRole)
        else if request.role == UserGroupRole.Owner then
          UserGroupTable.transferOwnership(connection, ownedGroup.value.id, currentOwnerUsername, targetUsername).flatMap {
            case UserGroupTable.UpdateMemberRoleTableResult.MemberNotFound =>
              IO.pure(UpdateUserGroupMemberRoleResult.MemberNotFound)
            case UserGroupTable.UpdateMemberRoleTableResult.Updated =>
              UserGroupTable.findBySlug(connection, ownedGroup.value.slug).map {
                case Some(updatedGroup) => UpdateUserGroupMemberRoleResult.Updated(updatedGroup)
                case None => throw new IllegalStateException("User group disappeared after ownership transfer")
              }
          }
        else
          UserGroupTable.updateMemberRole(connection, ownedGroup.value.id, targetUsername, request.role).flatMap {
            case UserGroupTable.UpdateMemberRoleTableResult.MemberNotFound =>
              IO.pure(UpdateUserGroupMemberRoleResult.MemberNotFound)
            case UserGroupTable.UpdateMemberRoleTableResult.Updated =>
              UserGroupTable.findBySlug(connection, ownedGroup.value.slug).map {
                case Some(updatedGroup) => UpdateUserGroupMemberRoleResult.Updated(updatedGroup)
                case None => throw new IllegalStateException("User group disappeared after member role update")
              }
          }
