package domains.blog.utils

import cats.effect.IO
import cats.syntax.all.*
import domains.auth.api.ResolveAccountUsername
import domains.blog.objects.request.{CreateBlogRequest, UpdateBlogRequest}
import domains.user.objects.Username
import domains.usergroup.api.ResolveUserGroupSlug
import domains.usergroup.objects.UserGroupSlug
import shared.api.HttpApiError
import shared.objects.access.{AccessSubject, ResourceVisibilityPolicy}

import java.sql.Connection

/** 博客可见性策略校验工具，负责 viewer 授权去重和主体存在性验证。 */
object BlogAccessPolicyValidation:

  /** 清理创建博客请求中的重复 viewer 授权主体。 */
  def sanitizePolicy(request: CreateBlogRequest): CreateBlogRequest =
    request.copy(visibilityPolicy = sanitizePolicy(request.visibilityPolicy))

  /** 清理更新博客请求中的重复 viewer 授权主体。 */
  def sanitizePolicy(request: UpdateBlogRequest): UpdateBlogRequest =
    request.copy(visibilityPolicy = sanitizePolicy(request.visibilityPolicy))

  /** 按主体类型与 key 去重 viewer 授权。 */
  def sanitizePolicy(visibilityPolicy: ResourceVisibilityPolicy): ResourceVisibilityPolicy =
    visibilityPolicy.copy(
      viewerGrants = visibilityPolicy.viewerGrants
        .distinctBy(subject => (subjectKind(subject), subjectKey(subject)))
    )

  /** 验证博客可见性策略中的 viewer 用户/用户组都存在。 */
  def validateVisibilityPolicySubjects(connection: Connection, policy: ResourceVisibilityPolicy): IO[Unit] =
    policy.viewerGrants.traverse_(validateVisibilityPolicySubject(connection, _))

  private def subjectKind(value: AccessSubject): String =
    value match
      case AccessSubject.User(_) => "user"
      case AccessSubject.UserGroup(_) => "user_group"

  private def subjectKey(value: AccessSubject): String =
    value match
      case AccessSubject.User(username) => username.value
      case AccessSubject.UserGroup(slug) => slug.value

  /** 校验单个授权主体存在；用户和用户组分别走对应解析 API。 */
  private def validateVisibilityPolicySubject(connection: Connection, subject: AccessSubject): IO[Unit] =
    subject match
      case AccessSubject.User(username) =>
        ResolveAccountUsername.plan(connection, Username(username.value)).flatMap { user =>
          HttpApiError.ensure(
            user.username.nonEmpty,
            HttpApiError.badRequest(s"Granted user not found: ${username.value}.")
          )
        }
      case AccessSubject.UserGroup(slug) =>
        ResolveUserGroupSlug.plan(connection, UserGroupSlug(slug.value)).flatMap { userGroup =>
          HttpApiError.ensure(
            userGroup.exists,
            HttpApiError.badRequest(s"Granted user group not found: ${slug.value}.")
          )
        }
