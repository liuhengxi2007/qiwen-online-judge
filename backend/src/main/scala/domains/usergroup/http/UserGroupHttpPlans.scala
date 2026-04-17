package domains.usergroup.http

import cats.effect.IO
import database.DatabaseSession
import domains.auth.model.{AuthUser, Username}
import domains.shared.model.{PageRequest, PageResponse}
import domains.shared.http.{PlainAuthenticatedHttpPlan, TransactionAuthenticatedHttpPlan}
import domains.usergroup.application.UserGroupCommands
import domains.usergroup.model.{AddUserGroupMemberRequest, CreateUserGroupRequest, UpdateUserGroupMemberRoleRequest, UpdateUserGroupRequest, UserGroupSlug}

import java.sql.Connection

object UserGroupHttpPlans:

  case object ListUserGroups extends PlainAuthenticatedHttpPlan[Unit, PageResponse[domains.usergroup.model.UserGroupSummary]]:

    override val name: String = "ListUserGroups"

    override def execute(
      databaseSession: DatabaseSession,
      actor: AuthUser,
      input: Unit
    ): IO[PageResponse[domains.usergroup.model.UserGroupSummary]] =
      UserGroupCommands.listUserGroups(databaseSession, actor, PageRequest())

  case object GetUserGroup extends PlainAuthenticatedHttpPlan[UserGroupSlug, UserGroupCommands.GetUserGroupResult]:

    override val name: String = "GetUserGroup"

    override def execute(
      databaseSession: DatabaseSession,
      actor: AuthUser,
      input: UserGroupSlug
    ): IO[UserGroupCommands.GetUserGroupResult] =
      UserGroupCommands.getUserGroupBySlug(databaseSession, actor, input)

  case object CreateUserGroup extends TransactionAuthenticatedHttpPlan[CreateUserGroupRequest, UserGroupCommands.CreateUserGroupResult]:

    override val name: String = "CreateUserGroup"

    override def execute(
      connection: Connection,
      actor: AuthUser,
      input: CreateUserGroupRequest
    ): IO[UserGroupCommands.CreateUserGroupResult] =
      UserGroupCommands.createUserGroup(connection, actor, input)

  case object UpdateUserGroup extends TransactionAuthenticatedHttpPlan[(UserGroupSlug, UpdateUserGroupRequest), UserGroupCommands.UpdateUserGroupResult]:

    override val name: String = "UpdateUserGroup"

    override def execute(
      connection: Connection,
      actor: AuthUser,
      input: (UserGroupSlug, UpdateUserGroupRequest)
    ): IO[UserGroupCommands.UpdateUserGroupResult] =
      val (slug, request) = input
      UserGroupCommands.updateUserGroup(connection, actor, slug, request)

  case object DeleteUserGroup extends TransactionAuthenticatedHttpPlan[UserGroupSlug, UserGroupCommands.DeleteUserGroupResult]:

    override val name: String = "DeleteUserGroup"

    override def execute(
      connection: Connection,
      actor: AuthUser,
      input: UserGroupSlug
    ): IO[UserGroupCommands.DeleteUserGroupResult] =
      UserGroupCommands.deleteUserGroup(connection, actor, input)

  case object AddMember extends TransactionAuthenticatedHttpPlan[(UserGroupSlug, AddUserGroupMemberRequest), UserGroupCommands.AddUserGroupMemberResult]:

    override val name: String = "AddMember"

    override def execute(
      connection: Connection,
      actor: AuthUser,
      input: (UserGroupSlug, AddUserGroupMemberRequest)
    ): IO[UserGroupCommands.AddUserGroupMemberResult] =
      val (slug, request) = input
      UserGroupCommands.addUserGroupMember(connection, actor, slug, request)

  case object UpdateMemberRole extends TransactionAuthenticatedHttpPlan[(UserGroupSlug, Username, UpdateUserGroupMemberRoleRequest), UserGroupCommands.UpdateUserGroupMemberRoleResult]:

    override val name: String = "UpdateMemberRole"

    override def execute(
      connection: Connection,
      actor: AuthUser,
      input: (UserGroupSlug, Username, UpdateUserGroupMemberRoleRequest)
    ): IO[UserGroupCommands.UpdateUserGroupMemberRoleResult] =
      val (slug, targetUsername, request) = input
      UserGroupCommands.updateUserGroupMemberRole(connection, actor, slug, targetUsername, request)

  case object RemoveMember extends TransactionAuthenticatedHttpPlan[(UserGroupSlug, Username), UserGroupCommands.RemoveUserGroupMemberResult]:

    override val name: String = "RemoveMember"

    override def execute(
      connection: Connection,
      actor: AuthUser,
      input: (UserGroupSlug, Username)
    ): IO[UserGroupCommands.RemoveUserGroupMemberResult] =
      val (slug, targetUsername) = input
      UserGroupCommands.removeUserGroupMember(connection, actor, slug, targetUsername)
