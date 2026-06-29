package domains.problem.api

import cats.effect.IO
import cats.syntax.all.*
import domains.auth.api.ResolveAccountUsername
import domains.problemset.api.ResolveProblemSetSlug
import domains.problemset.objects.ProblemSetSlug
import domains.user.objects.Username
import domains.usergroup.api.ResolveUserGroupSlug
import domains.usergroup.objects.UserGroupSlug
import shared.api.HttpApiError
import shared.objects.access.{AccessSubject, ResourceAccessPolicy}

import java.sql.Connection

/** 题目访问策略校验；负责确认授权主体和 slug 冲突资源真实存在。 */
object ProblemAccessPolicyValidation:

  /** 检查原始 slug 是否已被题单占用；非法题单 slug 视为不存在。 */
  def problemSetSlugExists(connection: Connection, rawSlug: String): IO[Boolean] =
    ProblemSetSlug.parse(rawSlug) match
      case Left(_) =>
        IO.pure(false)
      case Right(slug) =>
        ResolveProblemSetSlug.plan(connection, slug).map(_.exists)

  /** 校验访问策略中所有用户和用户组主体存在；不存在时返回 bad request。 */
  def validateAccessPolicySubjects(connection: Connection, policy: ResourceAccessPolicy): IO[Unit] =
    (policy.viewerGrants ++ policy.managerGrants).traverse_(validateAccessPolicySubject(connection, _))

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
