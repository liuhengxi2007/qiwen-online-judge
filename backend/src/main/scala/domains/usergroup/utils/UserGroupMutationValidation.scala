package domains.usergroup.utils

import cats.effect.IO
import domains.user.objects.Username
import domains.usergroup.objects.*
import domains.usergroup.objects.request.{AddUserGroupMemberRequest, CreateUserGroupRequest, UpdateUserGroupRequest}
import domains.usergroup.table.user_group.UserGroupTable

import java.sql.Connection

object UserGroupMutationValidation:

  def validateCreate(request: CreateUserGroupRequest): Either[String, CreateUserGroupRequest] =
    for
      slug <- UserGroupSlug.parse(request.slug.value)
      name <- UserGroupName.parse(request.name.value)
      description <- UserGroupDescription.parse(request.description.value)
    yield request.copy(slug = slug, name = name, description = description)

  def validateUpdate(request: UpdateUserGroupRequest): Either[String, UpdateUserGroupRequest] =
    for
      name <- UserGroupName.parse(request.name.value)
      description <- UserGroupDescription.parse(request.description.value)
    yield request.copy(name = name, description = description)

  def validateAddMember(request: AddUserGroupMemberRequest): Either[String, AddUserGroupMemberRequest] =
    Username.parse(request.username.value).map(username => request.copy(username = username))

  def updatedUserGroupOrError(message: String)(maybeGroup: Option[UserGroup]): UserGroup =
    maybeGroup.getOrElse(throw new IllegalStateException(message))

  def refreshBySlug(connection: Connection, slug: UserGroupSlug, message: String): IO[UserGroup] =
    UserGroupTable.findBySlug(connection, slug).map(updatedUserGroupOrError(message))
