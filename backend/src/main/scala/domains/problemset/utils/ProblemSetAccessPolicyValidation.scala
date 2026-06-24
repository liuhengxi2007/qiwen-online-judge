package domains.problemset.utils

import cats.effect.IO
import cats.syntax.all.*
import domains.auth.api.ResolveAccountUsername
import domains.problem.api.ResolveProblemReference
import domains.problem.objects.ProblemSlug
import domains.problemset.objects.request.{CreateProblemSetRequest, UpdateProblemSetRequest}
import domains.user.objects.Username
import domains.usergroup.api.ResolveUserGroupSlug
import domains.usergroup.objects.UserGroupSlug
import shared.api.HttpApiError
import shared.objects.access.{AccessSubject, ResourceVisibilityPolicy}

import java.sql.Connection

/** 题单访问策略校验工具，负责 viewer 授权去重和主体存在性验证。 */
object ProblemSetAccessPolicyValidation:

  /** 清理创建题单请求中的重复 viewer 授权主体。 */
  def sanitizePolicy(request: CreateProblemSetRequest): CreateProblemSetRequest =
    request.copy(accessPolicy = sanitizePolicy(request.accessPolicy))

  /** 清理更新题单请求中的重复 viewer 授权主体。 */
  def sanitizePolicy(request: UpdateProblemSetRequest): UpdateProblemSetRequest =
    request.copy(accessPolicy = sanitizePolicy(request.accessPolicy))

  /** 按主体类型与 key 去重 viewer 授权。 */
  def sanitizePolicy(accessPolicy: ResourceVisibilityPolicy): ResourceVisibilityPolicy =
    accessPolicy.copy(
      viewerGrants = accessPolicy.viewerGrants
        .distinctBy(subject => (subjectKind(subject), subjectKey(subject)))
    )

  private def subjectKind(value: AccessSubject): String =
    value match
      case AccessSubject.User(_) => "user"
      case AccessSubject.UserGroup(_) => "user_group"

  private def subjectKey(value: AccessSubject): String =
    value match
      case AccessSubject.User(username) => username.value
      case AccessSubject.UserGroup(slug) => slug.value

  /** 验证题单访问策略中的 viewer 用户/用户组都存在。 */
  def validateAccessPolicySubjects(connection: Connection, policy: ResourceVisibilityPolicy): IO[Unit] =
    policy.viewerGrants.traverse_(validateAccessPolicySubject(connection, _))

  /** 检查原始 slug 是否可解析为题目 slug 且对应题目存在，用于避免题单 slug 与题目路径冲突。 */
  def problemSlugExists(connection: Connection, rawSlug: String): IO[Boolean] =
    ProblemSlug.parse(rawSlug) match
      case Left(_) =>
        IO.pure(false)
      case Right(slug) =>
        ResolveProblemReference.plan(connection, slug).map(_.problem.nonEmpty)

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
