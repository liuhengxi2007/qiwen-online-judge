package domains.contest.utils

import cats.effect.IO
import cats.syntax.all.*
import domains.auth.api.ResolveAccountUsername
import domains.contest.objects.request.{CreateContestRequest, UpdateContestRequest}
import domains.user.objects.Username
import domains.usergroup.api.ResolveUserGroupSlug
import domains.usergroup.objects.UserGroupSlug
import shared.api.HttpApiError
import shared.objects.access.{AccessSubject, AccessUsername, ResourceAccessPolicy}

import java.sql.Connection

object ContestAccessPolicyValidation:

  def sanitizePolicy(request: CreateContestRequest): CreateContestRequest =
    request.copy(accessPolicy = sanitizePolicy(request.accessPolicy))

  def sanitizePolicyWithAuthorManager(request: CreateContestRequest, authorUsername: Username): CreateContestRequest =
    val authorManager = AccessSubject.User(AccessUsername(authorUsername.value))
    val policy = request.accessPolicy.copy(managerGrants = authorManager :: request.accessPolicy.managerGrants)
    request.copy(accessPolicy = sanitizePolicy(policy))

  def sanitizePolicyWithAuthorManager(request: UpdateContestRequest, authorUsername: Option[Username]): UpdateContestRequest =
    val authorManagerGrant = authorUsername.map(username => AccessSubject.User(AccessUsername(username.value))).toList
    val policy = request.accessPolicy.copy(managerGrants = authorManagerGrant ::: request.accessPolicy.managerGrants)
    request.copy(accessPolicy = sanitizePolicy(policy))

  def sanitizePolicy(accessPolicy: ResourceAccessPolicy): ResourceAccessPolicy =
    accessPolicy.copy(
      viewerGrants = accessPolicy.viewerGrants.distinctBy(subject => (subjectKind(subject), subjectKey(subject))),
      managerGrants = accessPolicy.managerGrants.distinctBy(subject => (subjectKind(subject), subjectKey(subject)))
    )

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

  private def subjectKind(value: AccessSubject): String =
    value match
      case AccessSubject.User(_) => "user"
      case AccessSubject.UserGroup(_) => "user_group"

  private def subjectKey(value: AccessSubject): String =
    value match
      case AccessSubject.User(username) => username.value
      case AccessSubject.UserGroup(slug) => slug.value
