package domains.usergroup.model



enum AddUserGroupMemberRole:
  case Manager
  case Member

object AddUserGroupMemberRole:
  def fromDatabase(value: String): Option[AddUserGroupMemberRole] =
    value match
      case "manager" => Some(AddUserGroupMemberRole.Manager)
      case "member" => Some(AddUserGroupMemberRole.Member)
      case _ => None

  def toDatabase(value: AddUserGroupMemberRole): String =
    value match
      case AddUserGroupMemberRole.Manager => "manager"
      case AddUserGroupMemberRole.Member => "member"
