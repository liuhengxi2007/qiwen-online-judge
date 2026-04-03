package domains.usergroup.http

import cats.effect.IO
import domains.shared.model.{ErrorResponse, SuccessResponse}
import domains.usergroup.application.UserGroupCommands
import io.circe.syntax.*
import org.http4s.{Response, Status}
import org.http4s.circe.CirceEntityEncoder.*

object UserGroupHttpResponses:

  def mapCreateResult(result: UserGroupCommands.CreateUserGroupResult): IO[Response[IO]] =
    result match
      case UserGroupCommands.CreateUserGroupResult.Forbidden =>
        errorResponse(Status.Forbidden, "User group creation is not allowed.")
      case UserGroupCommands.CreateUserGroupResult.ValidationFailed(message) =>
        errorResponse(Status.BadRequest, message)
      case UserGroupCommands.CreateUserGroupResult.SlugAlreadyExists =>
        errorResponse(Status.Conflict, "User group slug already exists.")
      case UserGroupCommands.CreateUserGroupResult.Created(group) =>
        IO.pure(Response[IO](status = Status.Created).withEntity(group.asJson))

  def mapGetResult(result: UserGroupCommands.GetUserGroupResult): IO[Response[IO]] =
    result match
      case UserGroupCommands.GetUserGroupResult.NotFound =>
        errorResponse(Status.NotFound, "User group not found.")
      case UserGroupCommands.GetUserGroupResult.Forbidden =>
        errorResponse(Status.Forbidden, "You do not have access to this user group.")
      case UserGroupCommands.GetUserGroupResult.Found(group) =>
        IO.pure(Response[IO](status = Status.Ok).withEntity(group.asJson))

  def mapUpdateResult(result: UserGroupCommands.UpdateUserGroupResult): IO[Response[IO]] =
    result match
      case UserGroupCommands.UpdateUserGroupResult.Forbidden =>
        errorResponse(Status.Forbidden, "Owner, manager, or site manager permission required.")
      case UserGroupCommands.UpdateUserGroupResult.ValidationFailed(message) =>
        errorResponse(Status.BadRequest, message)
      case UserGroupCommands.UpdateUserGroupResult.NotFound =>
        errorResponse(Status.NotFound, "User group not found.")
      case UserGroupCommands.UpdateUserGroupResult.Updated(group) =>
        IO.pure(Response[IO](status = Status.Ok).withEntity(group.asJson))

  def mapDeleteResult(result: UserGroupCommands.DeleteUserGroupResult): IO[Response[IO]] =
    result match
      case UserGroupCommands.DeleteUserGroupResult.Forbidden =>
        errorResponse(Status.Forbidden, "Owner or site manager permission required.")
      case UserGroupCommands.DeleteUserGroupResult.NotFound =>
        errorResponse(Status.NotFound, "User group not found.")
      case UserGroupCommands.DeleteUserGroupResult.Deleted =>
        IO.pure(Response[IO](status = Status.Ok).withEntity(SuccessResponse("User group deleted.").asJson))

  def mapAddMemberResult(result: UserGroupCommands.AddUserGroupMemberResult): IO[Response[IO]] =
    result match
      case UserGroupCommands.AddUserGroupMemberResult.Forbidden =>
        errorResponse(Status.Forbidden, "Owner, manager, or site manager permission required.")
      case UserGroupCommands.AddUserGroupMemberResult.ValidationFailed(message) =>
        errorResponse(Status.BadRequest, message)
      case UserGroupCommands.AddUserGroupMemberResult.UserGroupNotFound =>
        errorResponse(Status.NotFound, "User group not found.")
      case UserGroupCommands.AddUserGroupMemberResult.UserNotFound =>
        errorResponse(Status.NotFound, "User not found.")
      case UserGroupCommands.AddUserGroupMemberResult.MemberAlreadyExists =>
        errorResponse(Status.Conflict, "User is already a member of this group.")
      case UserGroupCommands.AddUserGroupMemberResult.Added(group) =>
        IO.pure(Response[IO](status = Status.Ok).withEntity(group.asJson))

  def mapUpdateMemberRoleResult(result: UserGroupCommands.UpdateUserGroupMemberRoleResult): IO[Response[IO]] =
    result match
      case UserGroupCommands.UpdateUserGroupMemberRoleResult.Forbidden =>
        errorResponse(Status.Forbidden, "Owner or site manager permission required.")
      case UserGroupCommands.UpdateUserGroupMemberRoleResult.ValidationFailed(message) =>
        errorResponse(Status.BadRequest, message)
      case UserGroupCommands.UpdateUserGroupMemberRoleResult.UserGroupNotFound =>
        errorResponse(Status.NotFound, "User group not found.")
      case UserGroupCommands.UpdateUserGroupMemberRoleResult.MemberNotFound =>
        errorResponse(Status.NotFound, "Group member not found.")
      case UserGroupCommands.UpdateUserGroupMemberRoleResult.CannotModifyOwnerRole =>
        errorResponse(Status.BadRequest, "The current owner cannot be modified directly. Transfer ownership instead.")
      case UserGroupCommands.UpdateUserGroupMemberRoleResult.OwnershipTransferRequired =>
        errorResponse(Status.BadRequest, "Ownership transfer is required.")
      case UserGroupCommands.UpdateUserGroupMemberRoleResult.Updated(group) =>
        IO.pure(Response[IO](status = Status.Ok).withEntity(group.asJson))

  private def errorResponse(status: Status, message: String): IO[Response[IO]] =
    IO.pure(Response[IO](status = status).withEntity(ErrorResponse(message).asJson))
