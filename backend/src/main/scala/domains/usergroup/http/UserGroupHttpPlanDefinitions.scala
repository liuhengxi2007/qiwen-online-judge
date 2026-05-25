package domains.usergroup.http

import domains.usergroup.http.mapper.UserGroupHttpResponseMappers



import domains.auth.model.AuthUser
import shared.http.AuthenticatedHttpPlanRegistry

object UserGroupHttpPlanDefinitions:

  import AuthenticatedHttpPlanRegistry.RegisteredPlan.{Plain, WithTransaction}

  private type PlainPlan[Input, Output] = Plain[AuthUser, Input, Output]
  private type TransactionPlan[Input, Output] = WithTransaction[AuthUser, Input, Output]

  val listUserGroups: PlainPlan[shared.model.PageRequest, shared.model.PageResponse[domains.usergroup.model.response.UserGroupSummary]] =
    Plain(UserGroupHttpPlans.ListUserGroups, UserGroupHttpResponseMappers.listUserGroupsResponse)
  val getUserGroup: PlainPlan[domains.usergroup.model.UserGroupSlug, domains.usergroup.application.UserGroupCommands.GetUserGroupResult] =
    Plain(UserGroupHttpPlans.GetUserGroup, UserGroupHttpResponseMappers.mapGetResult)
  val createUserGroup: TransactionPlan[domains.usergroup.model.request.CreateUserGroupRequest, domains.usergroup.application.UserGroupCommands.CreateUserGroupResult] =
    WithTransaction(UserGroupHttpPlans.CreateUserGroup, UserGroupHttpResponseMappers.mapCreateResult)
  val updateUserGroup: TransactionPlan[(domains.usergroup.model.UserGroupSlug, domains.usergroup.model.request.UpdateUserGroupRequest), domains.usergroup.application.UserGroupCommands.UpdateUserGroupResult] =
    WithTransaction(UserGroupHttpPlans.UpdateUserGroup, UserGroupHttpResponseMappers.mapUpdateResult)
  val deleteUserGroup: TransactionPlan[domains.usergroup.model.UserGroupSlug, domains.usergroup.application.UserGroupCommands.DeleteUserGroupResult] =
    WithTransaction(UserGroupHttpPlans.DeleteUserGroup, UserGroupHttpResponseMappers.mapDeleteResult)
  val addMember: TransactionPlan[(domains.usergroup.model.UserGroupSlug, domains.usergroup.model.request.AddUserGroupMemberRequest), domains.usergroup.application.UserGroupCommands.AddUserGroupMemberResult] =
    WithTransaction(UserGroupHttpPlans.AddMember, UserGroupHttpResponseMappers.mapAddMemberResult)
  val updateMemberRole: TransactionPlan[(domains.usergroup.model.UserGroupSlug, domains.user.model.Username, domains.usergroup.model.request.UpdateUserGroupMemberRoleRequest), domains.usergroup.application.UserGroupCommands.UpdateUserGroupMemberRoleResult] =
    WithTransaction(UserGroupHttpPlans.UpdateMemberRole, UserGroupHttpResponseMappers.mapUpdateMemberRoleResult)
  val removeMember: TransactionPlan[(domains.usergroup.model.UserGroupSlug, domains.user.model.Username), domains.usergroup.application.UserGroupCommands.RemoveUserGroupMemberResult] =
    WithTransaction(UserGroupHttpPlans.RemoveMember, UserGroupHttpResponseMappers.mapRemoveMemberResult)
