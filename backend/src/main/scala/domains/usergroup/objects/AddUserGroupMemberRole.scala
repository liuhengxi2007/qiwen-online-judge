package domains.usergroup.objects



enum AddUserGroupMemberRole:
  case Manager
  case Member

object AddUserGroupMemberRole:
  def parse(value: String): Either[String, AddUserGroupMemberRole] =
    value.trim match
      case "manager" => Right(AddUserGroupMemberRole.Manager)
      case "member" => Right(AddUserGroupMemberRole.Member)
      case _ => Left("Add-user-group-member role must be one of: manager, member.")
