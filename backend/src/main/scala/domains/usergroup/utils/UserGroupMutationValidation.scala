package domains.usergroup.utils

import cats.effect.IO
import domains.user.objects.Username
import domains.usergroup.objects.*
import domains.usergroup.objects.request.{AddUserGroupMemberRequest, CreateUserGroupRequest, UpdateUserGroupRequest}
import domains.usergroup.table.user_group.UserGroupTable

import java.sql.Connection

/** 用户组写操作输入校验和变更后刷新辅助。 */
object UserGroupMutationValidation:

  /** 校验创建请求中的 slug、名称和描述，并返回规范化后的请求。 */
  def validateCreate(request: CreateUserGroupRequest): Either[String, CreateUserGroupRequest] =
    for
      slug <- UserGroupSlug.parse(request.slug.value)
      name <- UserGroupName.parse(request.name.value)
      description <- UserGroupDescription.parse(request.description.value)
    yield request.copy(slug = slug, name = name, description = description)

  /** 校验更新请求中的名称和描述，并返回规范化后的请求。 */
  def validateUpdate(request: UpdateUserGroupRequest): Either[String, UpdateUserGroupRequest] =
    for
      name <- UserGroupName.parse(request.name.value)
      description <- UserGroupDescription.parse(request.description.value)
    yield request.copy(name = name, description = description)

  /** 校验新增成员用户名，并返回规范化后的请求。 */
  def validateAddMember(request: AddUserGroupMemberRequest): Either[String, AddUserGroupMemberRequest] =
    Username.parse(request.username.value).map(username => request.copy(username = username))

  /** 将变更后的重新查询结果转为用户组，缺失表示事务内状态异常。 */
  def updatedUserGroupOrError(message: String)(maybeGroup: Option[UserGroup]): UserGroup =
    maybeGroup.getOrElse(throw new IllegalStateException(message))

  /** 按 slug 刷新用户组详情，用于写操作后返回最新成员和时间字段。 */
  def refreshBySlug(connection: Connection, slug: UserGroupSlug, message: String): IO[UserGroup] =
    UserGroupTable.findBySlug(connection, slug).map(updatedUserGroupOrError(message))
