package domains.usergroup.application

import domains.auth.application.UsernameRules
import domains.auth.model.Username
import domains.usergroup.model.{AddUserGroupMemberRequest, CreateUserGroupRequest, UpdateUserGroupMemberRoleRequest, UpdateUserGroupRequest, UserGroupDescription, UserGroupName, UserGroupSlug}

object UserGroupValidation:

  private val slugPattern = "^[a-z0-9]+(?:-[a-z0-9]+)*$".r

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
    val normalized = slug.value.trim
    if normalized.isEmpty then Left("User group slug is required.")
    else if normalized.length < 3 || normalized.length > 64 then Left("User group slug must be between 3 and 64 characters.")
    else if !slugPattern.matches(normalized) then Left("User group slug may contain only lowercase letters, numbers, and hyphens.")
    else Right(UserGroupSlug(normalized))

  private def validateName(name: UserGroupName): Either[String, UserGroupName] =
    val normalized = name.value.trim
    if normalized.isEmpty then Left("User group name is required.")
    else if normalized.length > 120 then Left("User group name must be at most 120 characters.")
    else Right(UserGroupName(normalized))

  private def validateDescription(description: UserGroupDescription): Either[String, UserGroupDescription] =
    val normalized = description.value.trim
    if normalized.length > 2000 then Left("User group description must be at most 2000 characters.")
    else Right(UserGroupDescription(normalized))

  private def validateUsername(username: Username): Either[String, Username] =
    UsernameRules.validate(username) match
      case Some(message) => Left(message)
      case None => Right(Username.canonical(username.value))
