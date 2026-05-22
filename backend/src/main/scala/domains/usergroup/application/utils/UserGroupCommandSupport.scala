package domains.usergroup.application.utils



import domains.usergroup.application.{UserGroupCommands, UserGroupDecisions}
import cats.effect.IO
import domains.auth.model.AuthUser
import domains.user.model.Username
import domains.auth.table.AuthUserTable
import domains.usergroup.application.input.{AddUserGroupMemberRequest, UpdateUserGroupMemberRoleRequest, UpdateUserGroupRequest}
import domains.usergroup.model.{ManagedUserGroup, OwnedUserGroup, UserGroup}
import domains.usergroup.table.UserGroupTable

object UserGroupCommandSupport:

  def updateManagedUserGroup(
    connection: java.sql.Connection,
    managedGroup: ManagedUserGroup,
    request: UpdateUserGroupRequest
  ): IO[UserGroupCommands.UpdateUserGroupResult] =
    UserGroupTable.update(connection, managedGroup.value.id, request).flatMap { _ =>
      UserGroupTable
        .findBySlug(connection, managedGroup.value.slug)
        .map(updatedUserGroupOrError("User group disappeared after update"))
        .map(UserGroupCommands.UpdateUserGroupResult.Updated(_))
    }

  def addMemberToManagedUserGroup(
    connection: java.sql.Connection,
    managedGroup: ManagedUserGroup,
    request: AddUserGroupMemberRequest
  ): IO[UserGroupCommands.AddUserGroupMemberResult] =
    AuthUserTable.findByUsername(connection, request.username).flatMap {
      case None => IO.pure(UserGroupCommands.AddUserGroupMemberResult.UserNotFound)
      case Some(targetUser) =>
        UserGroupTable.addMember(connection, managedGroup.value.id, request.copy(username = targetUser.username)).flatMap {
          case UserGroupTable.AddMemberTableResult.AlreadyExists =>
            IO.pure(UserGroupCommands.AddUserGroupMemberResult.MemberAlreadyExists)
          case UserGroupTable.AddMemberTableResult.UserNotFound =>
            IO.pure(UserGroupCommands.AddUserGroupMemberResult.UserNotFound)
          case UserGroupTable.AddMemberTableResult.Added =>
            UserGroupTable
              .findBySlug(connection, managedGroup.value.slug)
              .map(updatedUserGroupOrError("User group disappeared after member add"))
              .map(UserGroupCommands.AddUserGroupMemberResult.Added(_))
        }
    }

  def deleteOwnedUserGroup(
    connection: java.sql.Connection,
    ownedGroup: OwnedUserGroup
  ): IO[UserGroupCommands.DeleteUserGroupResult] =
    UserGroupTable.delete(connection, ownedGroup.value.id).as(UserGroupCommands.DeleteUserGroupResult.Deleted)

  def updateMemberRoleForOwnedGroup(
    connection: java.sql.Connection,
    ownedGroup: OwnedUserGroup,
    targetUsername: Username,
    request: UpdateUserGroupMemberRoleRequest
  ): IO[UserGroupCommands.UpdateUserGroupMemberRoleResult] =
    UserGroupDecisions.decideMemberRoleUpdate(ownedGroup.value, targetUsername, request) match
      case UserGroupDecisions.MemberRoleUpdateDecision.MemberNotFound =>
        IO.pure(UserGroupCommands.UpdateUserGroupMemberRoleResult.MemberNotFound)
      case UserGroupDecisions.MemberRoleUpdateDecision.CannotModifyOwnerRole =>
        IO.pure(UserGroupCommands.UpdateUserGroupMemberRoleResult.CannotModifyOwnerRole)
      case UserGroupDecisions.MemberRoleUpdateDecision.TransferOwnership(canonicalTargetUsername) =>
        UserGroupTable.transferOwnership(
          connection,
          ownedGroup.value.id,
          ownedGroup.value.ownerUsername,
          canonicalTargetUsername,
        ).flatMap {
          case UserGroupTable.UpdateMemberRoleTableResult.MemberNotFound =>
            IO.pure(UserGroupCommands.UpdateUserGroupMemberRoleResult.MemberNotFound)
          case UserGroupTable.UpdateMemberRoleTableResult.Updated =>
            UserGroupTable
              .findBySlug(connection, ownedGroup.value.slug)
              .map(updatedUserGroupOrError("User group disappeared after ownership transfer"))
              .map(UserGroupCommands.UpdateUserGroupMemberRoleResult.Updated(_))
        }
      case UserGroupDecisions.MemberRoleUpdateDecision.UpdateRole(canonicalTargetUsername, role) =>
        UserGroupTable.updateMemberRole(connection, ownedGroup.value.id, canonicalTargetUsername, role).flatMap {
          case UserGroupTable.UpdateMemberRoleTableResult.MemberNotFound =>
            IO.pure(UserGroupCommands.UpdateUserGroupMemberRoleResult.MemberNotFound)
          case UserGroupTable.UpdateMemberRoleTableResult.Updated =>
            UserGroupTable
              .findBySlug(connection, ownedGroup.value.slug)
              .map(updatedUserGroupOrError("User group disappeared after member role update"))
              .map(UserGroupCommands.UpdateUserGroupMemberRoleResult.Updated(_))
        }

  def removeMemberFromGroup(
    connection: java.sql.Connection,
    actor: AuthUser,
    group: UserGroup,
    targetUsername: Username
  ): IO[UserGroupCommands.RemoveUserGroupMemberResult] =
    UserGroupDecisions.decideMemberRemoval(actor, group, targetUsername) match
      case UserGroupDecisions.MemberRemovalDecision.MemberNotFound =>
        IO.pure(UserGroupCommands.RemoveUserGroupMemberResult.MemberNotFound)
      case UserGroupDecisions.MemberRemovalDecision.CannotRemoveOwner =>
        IO.pure(UserGroupCommands.RemoveUserGroupMemberResult.CannotRemoveOwner)
      case UserGroupDecisions.MemberRemovalDecision.Forbidden =>
        IO.pure(UserGroupCommands.RemoveUserGroupMemberResult.Forbidden)
      case UserGroupDecisions.MemberRemovalDecision.Remove(canonicalTargetUsername) =>
        UserGroupTable.removeMember(connection, group.id, canonicalTargetUsername).flatMap {
          case UserGroupTable.RemoveMemberTableResult.MemberNotFound =>
            IO.pure(UserGroupCommands.RemoveUserGroupMemberResult.MemberNotFound)
          case UserGroupTable.RemoveMemberTableResult.Removed =>
            UserGroupTable
              .findBySlug(connection, group.slug)
              .map(updatedUserGroupOrError("User group disappeared after member removal"))
              .map(UserGroupCommands.RemoveUserGroupMemberResult.Removed(_))
        }

  def updatedUserGroupOrError(message: String)(maybeGroup: Option[UserGroup]): UserGroup =
    maybeGroup.getOrElse(throw new IllegalStateException(message))
