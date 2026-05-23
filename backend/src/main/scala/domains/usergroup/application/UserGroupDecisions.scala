package domains.usergroup.application



import domains.auth.model.AuthUser
import domains.user.model.Username
import domains.usergroup.model.{ManagedUserGroup, UserGroup, UserGroupRole}
import domains.usergroup.application.input.{UpdateUserGroupMemberRoleRequest}

object UserGroupDecisions:

  enum CreateUserGroupDecision:
    case SlugAlreadyExists
    case SlugConflictsWithUsername
    case Create

  enum UpdateUserGroupDecision:
    case NotFound
    case Forbidden
    case Update(managedGroup: ManagedUserGroup)

  enum MemberRoleUpdateDecision:
    case MemberNotFound
    case CannotModifyOwnerRole
    case TransferOwnership(targetUsername: Username)
    case UpdateRole(targetUsername: Username, role: UserGroupRole)

  enum MemberRemovalDecision:
    case MemberNotFound
    case CannotRemoveOwner
    case Forbidden
    case Remove(targetUsername: Username)

  def decideCreateUserGroup(
    existingGroup: Option[UserGroup],
    hasConflictingUsername: Boolean,
  ): CreateUserGroupDecision =
    existingGroup match
      case Some(_) => CreateUserGroupDecision.SlugAlreadyExists
      case None if hasConflictingUsername => CreateUserGroupDecision.SlugConflictsWithUsername
      case None => CreateUserGroupDecision.Create

  def decideUpdateUserGroup(
    actor: AuthUser,
    maybeGroup: Option[UserGroup],
  ): UpdateUserGroupDecision =
    maybeGroup match
      case None => UpdateUserGroupDecision.NotFound
      case Some(group) =>
        UserGroupPolicy.requireManaged(actor, group) match
          case None => UpdateUserGroupDecision.Forbidden
          case Some(managedGroup) => UpdateUserGroupDecision.Update(managedGroup)

  def decideMemberRoleUpdate(
    group: UserGroup,
    targetUsername: Username,
    request: UpdateUserGroupMemberRoleRequest,
  ): MemberRoleUpdateDecision =
    group.members.find(_.username.value == targetUsername.value) match
      case None =>
        MemberRoleUpdateDecision.MemberNotFound
      case Some(targetMember) if targetMember.role == UserGroupRole.Owner && request.role != UserGroupRole.Owner =>
        MemberRoleUpdateDecision.CannotModifyOwnerRole
      case Some(targetMember) if request.role == UserGroupRole.Owner =>
        MemberRoleUpdateDecision.TransferOwnership(targetMember.username)
      case Some(targetMember) =>
        MemberRoleUpdateDecision.UpdateRole(targetMember.username, request.role)

  def decideMemberRemoval(
    actor: AuthUser,
    group: UserGroup,
    targetUsername: Username,
  ): MemberRemovalDecision =
    group.members.find(_.username.value == targetUsername.value) match
      case None =>
        MemberRemovalDecision.MemberNotFound
      case Some(targetMember) if targetMember.role == UserGroupRole.Owner =>
        MemberRemovalDecision.CannotRemoveOwner
      case Some(targetMember) if !UserGroupPolicy.canRemoveMember(actor, group, targetMember.username, targetMember.role) =>
        MemberRemovalDecision.Forbidden
      case Some(targetMember) =>
        MemberRemovalDecision.Remove(targetMember.username)
