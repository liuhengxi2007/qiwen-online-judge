package domains.usergroup.application



import domains.auth.application.AuthCommands
import domains.user.model.Username
import domains.usergroup.model.request.{AddUserGroupMemberRequest, CreateUserGroupRequest, UpdateUserGroupMemberRoleRequest, UpdateUserGroupRequest}
import domains.usergroup.model.{UserGroupDescription, UserGroupName, UserGroupSlug}

object UserGroupValidation:

  def validateCreate(request: CreateUserGroupRequest): Either[String, CreateUserGroupRequest] =
    for
      slug <- validateSlug(request.slug)
      name <- validateName(request.name)
      description <- validateDescription(request.description)
    yield request.copy(slug = slug, name = name, description = description)

  def validateUpdate(request: UpdateUserGroupRequest): Either[String, UpdateUserGroupRequest] =
    for
      name <- validateName(request.name)
      description <- validateDescription(request.description)
    yield request.copy(name = name, description = description)

  def validateAddMember(request: AddUserGroupMemberRequest): Either[String, AddUserGroupMemberRequest] =
    validateUsername(request.username).map(validUsername => request.copy(username = validUsername))

  def validateUpdateMemberRole(request: UpdateUserGroupMemberRoleRequest): Either[String, UpdateUserGroupMemberRoleRequest] =
    Right(request)

  private def validateSlug(slug: UserGroupSlug): Either[String, UserGroupSlug] =
    UserGroupSlug.parse(slug.value)

  private def validateName(name: UserGroupName): Either[String, UserGroupName] =
    UserGroupName.parse(name.value)

  private def validateDescription(description: UserGroupDescription): Either[String, UserGroupDescription] =
    UserGroupDescription.parse(description.value)

  private def validateUsername(username: Username): Either[String, Username] =
    AuthCommands.validateUsername(username)
