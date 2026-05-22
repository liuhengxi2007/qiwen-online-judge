package domains.usergroup.application



import domains.usergroup.model.UserGroup

object UserGroupCommandResults:

  enum CreateUserGroupResult:
    case Forbidden
    case ValidationFailed(message: String)
    case SlugAlreadyExists
    case SlugConflictsWithUsername
    case Created(group: UserGroup)

  enum GetUserGroupResult:
    case NotFound
    case Forbidden
    case Found(group: UserGroup)

  enum UpdateUserGroupResult:
    case Forbidden
    case ValidationFailed(message: String)
    case NotFound
    case Updated(group: UserGroup)

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
    case Added(group: UserGroup)

  enum UpdateUserGroupMemberRoleResult:
    case Forbidden
    case ValidationFailed(message: String)
    case UserGroupNotFound
    case MemberNotFound
    case CannotModifyOwnerRole
    case OwnershipTransferRequired
    case Updated(group: UserGroup)

  enum RemoveUserGroupMemberResult:
    case Forbidden
    case UserGroupNotFound
    case MemberNotFound
    case CannotRemoveOwner
    case Removed(group: UserGroup)
