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
import shared.objects.access.{AccessSubject, ResourceAccessPolicy}

import java.sql.Connection

/** 题单访问策略校验工具，负责 viewer 授权去重、禁止 manager 授权和主体存在性验证。 */
object ProblemSetAccessPolicyValidation:

  /** 清理创建题单请求中的重复授权主体，并丢弃 manager 授权。 */
  def sanitizePolicy(request: CreateProblemSetRequest): CreateProblemSetRequest =
    request.copy(accessPolicy = sanitizePolicy(request.accessPolicy))

  /** 清理更新题单请求中的重复授权主体，并丢弃 manager 授权。 */
  def sanitizePolicy(request: UpdateProblemSetRequest): UpdateProblemSetRequest =
    request.copy(accessPolicy = sanitizePolicy(request.accessPolicy))

  /** 只保留 viewer 授权并按主体类型与 key 去重，题单不支持资源级 manager 授权。 */
  def sanitizePolicy(accessPolicy: ResourceAccessPolicy): ResourceAccessPolicy =
    accessPolicy.copy(
      viewerGrants = accessPolicy.viewerGrants
        .distinctBy(subject => (subjectKind(subject), subjectKey(subject))),
      managerGrants = Nil
    )

  private def subjectKind(value: AccessSubject): String =
    value match
      case AccessSubject.User(_) => "user"
      case AccessSubject.UserGroup(_) => "user_group"

  private def subjectKey(value: AccessSubject): String =
    value match
      case AccessSubject.User(username) => username.value
      case AccessSubject.UserGroup(slug) => slug.value

  /** 验证题单访问策略不包含 manager 授权，且 viewer 用户/用户组都存在。 */
  def validateAccessPolicySubjects(connection: Connection, policy: ResourceAccessPolicy): IO[Unit] =
    for
      _ <- HttpApiError.ensure(
        policy.managerGrants.isEmpty,
        HttpApiError.badRequest("Problem set access policies do not support manager grants.")
      )
      _ <- policy.viewerGrants.traverse_(validateAccessPolicySubject(connection, _))
    yield ()

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
