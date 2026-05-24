package domains.usergroup.http

import domains.usergroup.http.response.UserGroupHttpResponses



import domains.auth.model.AuthUser
import shared.http.AuthenticatedHttpPlanRegistry

object UserGroupHttpPlanDefinitions:

  import AuthenticatedHttpPlanRegistry.RegisteredPlan.{Plain, WithTransaction}

  private type PlainPlan[Input, Output] = Plain[AuthUser, Input, Output]
  private type TransactionPlan[Input, Output] = WithTransaction[AuthUser, Input, Output]

  val listUserGroups: PlainPlan[shared.model.PageRequest, shared.model.PageResponse[domains.usergroup.application.output.UserGroupSummary]] =
    Plain(UserGroupHttpPlans.ListUserGroups, UserGroupHttpResponses.listUserGroupsResponse)
  val getUserGroup: PlainPlan[domains.usergroup.model.UserGroupSlug, domains.usergroup.application.UserGroupCommands.GetUserGroupResult] =
    Plain(UserGroupHttpPlans.GetUserGroup, UserGroupHttpResponses.mapGetResult)
  val createUserGroup: TransactionPlan[domains.usergroup.application.input.CreateUserGroupRequest, domains.usergroup.application.UserGroupCommands.CreateUserGroupResult] =
    WithTransaction(UserGroupHttpPlans.CreateUserGroup, UserGroupHttpResponses.mapCreateResult)
  val updateUserGroup: TransactionPlan[(domains.usergroup.model.UserGroupSlug, domains.usergroup.application.input.UpdateUserGroupRequest), domains.usergroup.application.UserGroupCommands.UpdateUserGroupResult] =
    WithTransaction(UserGroupHttpPlans.UpdateUserGroup, UserGroupHttpResponses.mapUpdateResult)
  val deleteUserGroup: TransactionPlan[domains.usergroup.model.UserGroupSlug, domains.usergroup.application.UserGroupCommands.DeleteUserGroupResult] =
    WithTransaction(UserGroupHttpPlans.DeleteUserGroup, UserGroupHttpResponses.mapDeleteResult)
  val addMember: TransactionPlan[(domains.usergroup.model.UserGroupSlug, domains.usergroup.application.input.AddUserGroupMemberRequest), domains.usergroup.application.UserGroupCommands.AddUserGroupMemberResult] =
    WithTransaction(UserGroupHttpPlans.AddMember, UserGroupHttpResponses.mapAddMemberResult)
  val updateMemberRole: TransactionPlan[(domains.usergroup.model.UserGroupSlug, domains.user.model.Username, domains.usergroup.application.input.UpdateUserGroupMemberRoleRequest), domains.usergroup.application.UserGroupCommands.UpdateUserGroupMemberRoleResult] =
    WithTransaction(UserGroupHttpPlans.UpdateMemberRole, UserGroupHttpResponses.mapUpdateMemberRoleResult)
  val removeMember: TransactionPlan[(domains.usergroup.model.UserGroupSlug, domains.user.model.Username), domains.usergroup.application.UserGroupCommands.RemoveUserGroupMemberResult] =
    WithTransaction(UserGroupHttpPlans.RemoveMember, UserGroupHttpResponses.mapRemoveMemberResult)

  val plans: Map[String, AuthenticatedHttpPlanRegistry.RegisteredPlan] =
    List(
      listUserGroups,
      getUserGroup,
      createUserGroup,
      updateUserGroup,
      deleteUserGroup,
      addMember,
      updateMemberRole,
      removeMember
    ).map(plan => plan.name -> plan).toMap
