package domains.problemset.utils

import cats.effect.IO
import cats.syntax.all.*
import domains.auth.table.auth_user.AuthUserTable
import domains.problem.objects.ProblemSlug
import domains.problem.table.problem.ProblemQueryTable
import domains.problemset.objects.request.{CreateProblemSetRequest, UpdateProblemSetRequest}
import domains.user.objects.Username
import domains.usergroup.objects.UserGroupSlug
import domains.usergroup.table.user_group.UserGroupTable
import shared.api.HttpApiError
import shared.objects.access.{AccessSubject, ResourceAccessPolicy}

import java.sql.Connection

object ProblemSetAccessPolicyValidation:

  def sanitizePolicy(request: CreateProblemSetRequest): CreateProblemSetRequest =
    request.copy(accessPolicy = sanitizePolicy(request.accessPolicy))

  def sanitizePolicy(request: UpdateProblemSetRequest): UpdateProblemSetRequest =
    request.copy(accessPolicy = sanitizePolicy(request.accessPolicy))

  def sanitizePolicy(accessPolicy: ResourceAccessPolicy): ResourceAccessPolicy =
    accessPolicy.copy(
      viewerGrants = accessPolicy.viewerGrants
        .distinctBy(subject => (AccessSubject.subjectKind(subject), AccessSubject.subjectKey(subject))),
      managerGrants = accessPolicy.managerGrants
        .distinctBy(subject => (AccessSubject.subjectKind(subject), AccessSubject.subjectKey(subject)))
    )

  def validateAccessPolicySubjects(connection: Connection, policy: ResourceAccessPolicy): IO[Unit] =
    (policy.viewerGrants ++ policy.managerGrants).traverse_(validateAccessPolicySubject(connection, _))

  def problemSlugExists(connection: Connection, rawSlug: String): IO[Boolean] =
    ProblemSlug.parse(rawSlug) match
      case Left(_) =>
        IO.pure(false)
      case Right(slug) =>
        ProblemQueryTable.findBySlug(connection, slug).map(_.nonEmpty)

  private def validateAccessPolicySubject(connection: Connection, subject: AccessSubject): IO[Unit] =
    subject match
      case AccessSubject.User(username) =>
        AuthUserTable.findByUsername(connection, Username(username.value)).flatMap { user =>
          HttpApiError.ensure(
            user.nonEmpty,
            HttpApiError.badRequest(s"Granted user not found: ${username.value}.")
          )
        }
      case AccessSubject.UserGroup(slug) =>
        UserGroupTable.findBySlug(connection, UserGroupSlug(slug.value)).flatMap { userGroup =>
          HttpApiError.ensure(
            userGroup.nonEmpty,
            HttpApiError.badRequest(s"Granted user group not found: ${slug.value}.")
          )
        }
