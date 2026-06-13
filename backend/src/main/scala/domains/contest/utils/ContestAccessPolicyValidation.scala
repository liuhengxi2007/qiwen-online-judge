package domains.contest.utils

import cats.effect.IO
import cats.syntax.all.*
import domains.auth.api.ResolveAccountUsername
import domains.contest.objects.request.{CreateContestRequest, UpdateContestRequest}
import domains.user.objects.Username
import domains.usergroup.api.ResolveUserGroupSlug
import domains.usergroup.objects.UserGroupSlug
import shared.api.HttpApiError
import shared.objects.access.{AccessSubject, ResourceAccessPolicy}

import java.sql.Connection

/** 比赛访问策略校验工具，负责授权主体去重和用户/用户组存在性验证。 */
object ContestAccessPolicyValidation:

  /** 清理创建比赛请求中的重复授权主体。 */
  def sanitizePolicy(request: CreateContestRequest): CreateContestRequest =
    request.copy(accessPolicy = sanitizePolicy(request.accessPolicy))

  /** 清理更新比赛请求中的重复授权主体。 */
  def sanitizePolicy(request: UpdateContestRequest): UpdateContestRequest =
    request.copy(accessPolicy = sanitizePolicy(request.accessPolicy))

  /** 对 viewer 和 manager 授权按主体类型与 key 去重，保留原顺序中的首次出现。 */
  def sanitizePolicy(accessPolicy: ResourceAccessPolicy): ResourceAccessPolicy =
    accessPolicy.copy(
      viewerGrants = accessPolicy.viewerGrants.distinctBy(subject => (subjectKind(subject), subjectKey(subject))),
      managerGrants = accessPolicy.managerGrants.distinctBy(subject => (subjectKind(subject), subjectKey(subject)))
    )

  /** 验证访问策略中的用户和用户组都存在，失败时返回 400。 */
  def validateAccessPolicySubjects(connection: Connection, policy: ResourceAccessPolicy): IO[Unit] =
    (policy.viewerGrants ++ policy.managerGrants).traverse_(validateAccessPolicySubject(connection, _))

  /** 校验单个访问策略主体存在；用户和用户组分别走对应解析 API。 */
  private def validateAccessPolicySubject(connection: Connection, subject: AccessSubject): IO[Unit] =
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

  private def subjectKind(value: AccessSubject): String =
    value match
      case AccessSubject.User(_) => "user"
      case AccessSubject.UserGroup(_) => "user_group"

  private def subjectKey(value: AccessSubject): String =
    value match
      case AccessSubject.User(username) => username.value
      case AccessSubject.UserGroup(slug) => slug.value
