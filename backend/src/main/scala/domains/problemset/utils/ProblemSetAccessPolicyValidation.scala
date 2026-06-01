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

object ProblemSetAccessPolicyValidation:

  def sanitizePolicy(request: CreateProblemSetRequest): CreateProblemSetRequest =
    request.copy(accessPolicy = sanitizePolicy(request.accessPolicy))

  def sanitizePolicy(request: UpdateProblemSetRequest): UpdateProblemSetRequest =
    request.copy(accessPolicy = sanitizePolicy(request.accessPolicy))

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

  def validateAccessPolicySubjects(connection: Connection, policy: ResourceAccessPolicy): IO[Unit] =
    for
      _ <- HttpApiError.ensure(
        policy.managerGrants.isEmpty,
        HttpApiError.badRequest("Problem set access policies do not support manager grants.")
      )
      _ <- policy.viewerGrants.traverse_(validateAccessPolicySubject(connection, _))
    yield ()

  def problemSlugExists(connection: Connection, rawSlug: String): IO[Boolean] =
    ProblemSlug.parse(rawSlug) match
      case Left(_) =>
        IO.pure(false)
      case Right(slug) =>
        ResolveProblemReference.plan(connection, slug).map(_.problem.nonEmpty)

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
