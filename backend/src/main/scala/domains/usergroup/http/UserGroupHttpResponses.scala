package domains.usergroup.http

import cats.effect.IO
import domains.shared.http.ApiMessages
import domains.shared.http.HttpResponseSupport.{errorResponse, successResponse, validationErrorResponse}
import domains.usergroup.application.UserGroupCommands
import domains.usergroup.model.{UserGroup, UserGroupDetail}
import io.circe.syntax.*
import org.http4s.{Response, Status}
import org.http4s.circe.CirceEntityEncoder.*

object UserGroupHttpResponses:

  private def hiddenUserGroupResponse: IO[Response[IO]] =
    errorResponse(Status.NotFound, ApiMessages.userGroupNotFound)

  def validationErrorResponse(message: String): IO[Response[IO]] =
    domains.shared.http.HttpResponseSupport.validationErrorResponse(message)

  def listUserGroupsResponse(response: domains.shared.model.PageResponse[domains.usergroup.model.UserGroupSummary]): IO[Response[IO]] =
    IO.pure(Response[IO](status = Status.Ok).withEntity(response.asJson))

  def toUserGroupDetail(group: UserGroup): UserGroupDetail =
    UserGroupDetail(
      id = group.id,
      slug = group.slug,
      name = group.name,
      description = group.description,
      ownerUsername = group.ownerUsername,
      members = group.members,
      createdAt = group.createdAt,
      updatedAt = group.updatedAt
    )

  def mapCreateResult(result: UserGroupCommands.CreateUserGroupResult): IO[Response[IO]] =
    result match
      case UserGroupCommands.CreateUserGroupResult.Forbidden =>
        errorResponse(Status.Forbidden, ApiMessages.userGroupCreationForbidden)
      case UserGroupCommands.CreateUserGroupResult.ValidationFailed(message) =>
        errorResponse(Status.BadRequest, message)
      case UserGroupCommands.CreateUserGroupResult.SlugAlreadyExists =>
        errorResponse(Status.Conflict, ApiMessages.userGroupSlugExists)
      case UserGroupCommands.CreateUserGroupResult.SlugConflictsWithUsername =>
        errorResponse(Status.Conflict, ApiMessages.userGroupSlugConflictsWithUsername)
      case UserGroupCommands.CreateUserGroupResult.Created(group) =>
        IO.pure(Response[IO](status = Status.Created).withEntity(toUserGroupDetail(group).asJson))

  def mapGetResult(result: UserGroupCommands.GetUserGroupResult): IO[Response[IO]] =
    result match
      case UserGroupCommands.GetUserGroupResult.NotFound =>
        hiddenUserGroupResponse
      case UserGroupCommands.GetUserGroupResult.Forbidden =>
        hiddenUserGroupResponse
      case UserGroupCommands.GetUserGroupResult.Found(group) =>
        IO.pure(Response[IO](status = Status.Ok).withEntity(toUserGroupDetail(group).asJson))

  def mapUpdateResult(result: UserGroupCommands.UpdateUserGroupResult): IO[Response[IO]] =
    result match
      case UserGroupCommands.UpdateUserGroupResult.Forbidden =>
        hiddenUserGroupResponse
      case UserGroupCommands.UpdateUserGroupResult.ValidationFailed(message) =>
        errorResponse(Status.BadRequest, message)
      case UserGroupCommands.UpdateUserGroupResult.NotFound =>
        hiddenUserGroupResponse
      case UserGroupCommands.UpdateUserGroupResult.Updated(group) =>
        IO.pure(Response[IO](status = Status.Ok).withEntity(toUserGroupDetail(group).asJson))

  def mapDeleteResult(result: UserGroupCommands.DeleteUserGroupResult): IO[Response[IO]] =
    result match
      case UserGroupCommands.DeleteUserGroupResult.Forbidden =>
        hiddenUserGroupResponse
      case UserGroupCommands.DeleteUserGroupResult.NotFound =>
        hiddenUserGroupResponse
      case UserGroupCommands.DeleteUserGroupResult.Deleted =>
        successResponse(Status.Ok, ApiMessages.userGroupDeleted)

  def mapAddMemberResult(result: UserGroupCommands.AddUserGroupMemberResult): IO[Response[IO]] =
    result match
      case UserGroupCommands.AddUserGroupMemberResult.Forbidden =>
        hiddenUserGroupResponse
      case UserGroupCommands.AddUserGroupMemberResult.ValidationFailed(message) =>
        errorResponse(Status.BadRequest, message)
      case UserGroupCommands.AddUserGroupMemberResult.UserGroupNotFound =>
        hiddenUserGroupResponse
      case UserGroupCommands.AddUserGroupMemberResult.UserNotFound =>
        errorResponse(Status.NotFound, ApiMessages.userNotFound)
      case UserGroupCommands.AddUserGroupMemberResult.MemberAlreadyExists =>
        errorResponse(Status.Conflict, ApiMessages.userAlreadyMemberOfGroup)
      case UserGroupCommands.AddUserGroupMemberResult.Added(group) =>
        IO.pure(Response[IO](status = Status.Ok).withEntity(toUserGroupDetail(group).asJson))

  def mapUpdateMemberRoleResult(result: UserGroupCommands.UpdateUserGroupMemberRoleResult): IO[Response[IO]] =
    result match
      case UserGroupCommands.UpdateUserGroupMemberRoleResult.Forbidden =>
        hiddenUserGroupResponse
      case UserGroupCommands.UpdateUserGroupMemberRoleResult.ValidationFailed(message) =>
        errorResponse(Status.BadRequest, message)
      case UserGroupCommands.UpdateUserGroupMemberRoleResult.UserGroupNotFound =>
        hiddenUserGroupResponse
      case UserGroupCommands.UpdateUserGroupMemberRoleResult.MemberNotFound =>
        errorResponse(Status.NotFound, ApiMessages.groupMemberNotFound)
      case UserGroupCommands.UpdateUserGroupMemberRoleResult.CannotModifyOwnerRole =>
        errorResponse(Status.BadRequest, ApiMessages.userGroupOwnerModifyForbidden)
      case UserGroupCommands.UpdateUserGroupMemberRoleResult.OwnershipTransferRequired =>
        errorResponse(Status.BadRequest, ApiMessages.ownershipTransferRequired)
      case UserGroupCommands.UpdateUserGroupMemberRoleResult.Updated(group) =>
        IO.pure(Response[IO](status = Status.Ok).withEntity(toUserGroupDetail(group).asJson))

  def mapRemoveMemberResult(result: UserGroupCommands.RemoveUserGroupMemberResult): IO[Response[IO]] =
    result match
      case UserGroupCommands.RemoveUserGroupMemberResult.Forbidden =>
        hiddenUserGroupResponse
      case UserGroupCommands.RemoveUserGroupMemberResult.UserGroupNotFound =>
        hiddenUserGroupResponse
      case UserGroupCommands.RemoveUserGroupMemberResult.MemberNotFound =>
        errorResponse(Status.NotFound, ApiMessages.groupMemberNotFound)
      case UserGroupCommands.RemoveUserGroupMemberResult.CannotRemoveOwner =>
        errorResponse(Status.BadRequest, ApiMessages.userGroupOwnerRemoveForbidden)
      case UserGroupCommands.RemoveUserGroupMemberResult.Removed(group) =>
        IO.pure(Response[IO](status = Status.Ok).withEntity(toUserGroupDetail(group).asJson))
