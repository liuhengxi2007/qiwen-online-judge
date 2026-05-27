package domains.usergroup.http



import cats.effect.IO
import database.DatabaseSession
import domains.auth.objects.AuthUser
import domains.user.objects.Username
import shared.objects.{PageRequest, PageResponse}
import shared.http.{PlainAuthenticatedHttpPlan, TransactionAuthenticatedHttpPlan}
import domains.usergroup.application.UserGroupCommands
import domains.usergroup.objects.request.{AddUserGroupMemberRequest, CreateUserGroupRequest, UpdateUserGroupMemberRoleRequest, UpdateUserGroupRequest}
import domains.usergroup.objects.{UserGroupSlug}

import java.sql.Connection

object UserGroupHttpPlans:

  case object ListUserGroups extends PlainAuthenticatedHttpPlan[AuthUser, PageRequest, PageResponse[domains.usergroup.objects.response.UserGroupSummary]]:

    override def execute(
      databaseSession: DatabaseSession,
      actor: AuthUser,
      input: PageRequest
    ): IO[PageResponse[domains.usergroup.objects.response.UserGroupSummary]] =
      UserGroupCommands.listUserGroups(databaseSession, actor, input)

  case object GetUserGroup extends PlainAuthenticatedHttpPlan[AuthUser, UserGroupSlug, UserGroupCommands.GetUserGroupResult]:

    override def execute(
      databaseSession: DatabaseSession,
      actor: AuthUser,
      input: UserGroupSlug
    ): IO[UserGroupCommands.GetUserGroupResult] =
      UserGroupCommands.getUserGroupBySlug(databaseSession, actor, input)

  case object CreateUserGroup extends TransactionAuthenticatedHttpPlan[AuthUser, CreateUserGroupRequest, UserGroupCommands.CreateUserGroupResult]:

    override def execute(
      connection: Connection,
      actor: AuthUser,
      input: CreateUserGroupRequest
    ): IO[UserGroupCommands.CreateUserGroupResult] =
      UserGroupCommands.createUserGroup(connection, actor, input)

  case object UpdateUserGroup extends TransactionAuthenticatedHttpPlan[AuthUser, (UserGroupSlug, UpdateUserGroupRequest), UserGroupCommands.UpdateUserGroupResult]:

    override def execute(
      connection: Connection,
      actor: AuthUser,
      input: (UserGroupSlug, UpdateUserGroupRequest)
    ): IO[UserGroupCommands.UpdateUserGroupResult] =
      val (slug, request) = input
      UserGroupCommands.updateUserGroup(connection, actor, slug, request)

  case object DeleteUserGroup extends TransactionAuthenticatedHttpPlan[AuthUser, UserGroupSlug, UserGroupCommands.DeleteUserGroupResult]:

    override def execute(
      connection: Connection,
      actor: AuthUser,
      input: UserGroupSlug
    ): IO[UserGroupCommands.DeleteUserGroupResult] =
      UserGroupCommands.deleteUserGroup(connection, actor, input)

  case object AddMember extends TransactionAuthenticatedHttpPlan[AuthUser, (UserGroupSlug, AddUserGroupMemberRequest), UserGroupCommands.AddUserGroupMemberResult]:

    override def execute(
      connection: Connection,
      actor: AuthUser,
      input: (UserGroupSlug, AddUserGroupMemberRequest)
    ): IO[UserGroupCommands.AddUserGroupMemberResult] =
      val (slug, request) = input
      UserGroupCommands.addUserGroupMember(connection, actor, slug, request)

  case object UpdateMemberRole extends TransactionAuthenticatedHttpPlan[AuthUser, (UserGroupSlug, Username, UpdateUserGroupMemberRoleRequest), UserGroupCommands.UpdateUserGroupMemberRoleResult]:

    override def execute(
      connection: Connection,
      actor: AuthUser,
      input: (UserGroupSlug, Username, UpdateUserGroupMemberRoleRequest)
    ): IO[UserGroupCommands.UpdateUserGroupMemberRoleResult] =
      val (slug, targetUsername, request) = input
      UserGroupCommands.updateUserGroupMemberRole(connection, actor, slug, targetUsername, request)

  case object RemoveMember extends TransactionAuthenticatedHttpPlan[AuthUser, (UserGroupSlug, Username), UserGroupCommands.RemoveUserGroupMemberResult]:

    override def execute(
      connection: Connection,
      actor: AuthUser,
      input: (UserGroupSlug, Username)
    ): IO[UserGroupCommands.RemoveUserGroupMemberResult] =
      val (slug, targetUsername) = input
      UserGroupCommands.removeUserGroupMember(connection, actor, slug, targetUsername)
