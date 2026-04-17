package domains.usergroup.http

object UserGroupHttpPlanDefinitions:

  import UserGroupHttpPlanRegistry.RegisteredPlan.{Plain, WithTransaction}

  val listUserGroups = Plain(UserGroupHttpPlans.ListUserGroups, UserGroupHttpResponses.listUserGroupsResponse)
  val getUserGroup = Plain(UserGroupHttpPlans.GetUserGroup, UserGroupHttpResponses.mapGetResult)
  val createUserGroup = WithTransaction(UserGroupHttpPlans.CreateUserGroup, UserGroupHttpResponses.mapCreateResult)
  val updateUserGroup = WithTransaction(UserGroupHttpPlans.UpdateUserGroup, UserGroupHttpResponses.mapUpdateResult)
  val deleteUserGroup = WithTransaction(UserGroupHttpPlans.DeleteUserGroup, UserGroupHttpResponses.mapDeleteResult)
  val addMember = WithTransaction(UserGroupHttpPlans.AddMember, UserGroupHttpResponses.mapAddMemberResult)
  val updateMemberRole = WithTransaction(UserGroupHttpPlans.UpdateMemberRole, UserGroupHttpResponses.mapUpdateMemberRoleResult)
  val removeMember = WithTransaction(UserGroupHttpPlans.RemoveMember, UserGroupHttpResponses.mapRemoveMemberResult)

  val plans: Map[String, UserGroupHttpPlanRegistry.RegisteredPlan] =
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
