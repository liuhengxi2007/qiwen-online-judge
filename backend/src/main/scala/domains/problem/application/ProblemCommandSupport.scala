package domains.problem.application

import cats.effect.IO
import domains.auth.model.AuthUser
import domains.auth.table.AuthUserTable
import domains.problem.model.{CreateProblemRequest, ProblemDetail, UpdateProblemRequest}
import domains.problem.table.ProblemTable
import domains.problemset.model.{ProblemSet, ProblemSetSlug}
import domains.problemset.table.ProblemSetTable
import domains.shared.access.{AccessSubject, ResourceAccessFacts, ResourceAccessPolicy}
import domains.usergroup.table.UserGroupTable

object ProblemCommandSupport:

  def findConflictingProblemSet(
    connection: java.sql.Connection,
    rawSlug: String
  ): IO[Option[ProblemSet]] =
    ProblemSetSlug.parse(rawSlug) match
      case Left(_) => IO.pure(None)
      case Right(slug) => ProblemSetTable.findBySlug(connection, slug)

  def updatedProblemOrError(message: String)(maybeProblem: Option[ProblemDetail]): ProblemDetail =
    maybeProblem.getOrElse(throw new IllegalStateException(message))

  def enrichProblemPermissions(
    connection: java.sql.Connection,
    actor: AuthUser,
    problem: ProblemDetail
  ): IO[Option[ProblemDetail]] =
    UserGroupTable.listGroupSlugsForMember(connection, actor.username).flatMap { actorGroupSlugs =>
      val resourceAccessFacts = ResourceAccessFacts(
        policy = problem.accessPolicy,
        actorUsername = actor.username,
        actorGroupSlugs = actorGroupSlugs,
        hasGlobalViewOverride = ProblemPolicy.hasGlobalViewOverride(actor),
        hasGlobalManageOverride = ProblemPolicy.hasGlobalManageOverride(actor)
      )

      ProblemTable.hasVisibleContainingProblemSet(connection, actor, problem.id).map { hasVisibleContainingProblemSet =>
        val decision = ProblemAccessDecision.evaluate(
          ProblemAccessFacts(
            resourceAccess = resourceAccessFacts,
            hasVisibleContainingProblemSet = hasVisibleContainingProblemSet
          )
        )

        if decision.canView then Some(problem.copy(canManage = decision.canManage)) else None
      }
    }

  def canManageProblem(
    connection: java.sql.Connection,
    actor: AuthUser,
    problem: ProblemDetail
  ): IO[Boolean] =
    UserGroupTable.listGroupSlugsForMember(connection, actor.username).map { actorGroupSlugs =>
      ProblemAccessDecision
        .evaluate(
          ProblemAccessFacts(
            resourceAccess = ResourceAccessFacts(
              policy = problem.accessPolicy,
              actorUsername = actor.username,
              actorGroupSlugs = actorGroupSlugs,
              hasGlobalViewOverride = ProblemPolicy.hasGlobalViewOverride(actor),
              hasGlobalManageOverride = ProblemPolicy.hasGlobalManageOverride(actor)
            ),
            hasVisibleContainingProblemSet = false
          )
        )
        .canManage
    }

  def validateAccessPolicySubjects(
    connection: java.sql.Connection,
    policy: ResourceAccessPolicy
  ): IO[Option[String]] =
    (policy.viewerGrants ++ policy.managerGrants).foldLeft(IO.pure(Option.empty[String])) { (accIO, subject) =>
      accIO.flatMap {
        case some @ Some(_) => IO.pure(some)
        case None =>
          subject match
            case AccessSubject.User(username) =>
              AuthUserTable.findByUsername(connection, username).map {
                case Some(_) => None
                case None => Some(s"Granted user not found: ${username.value}.")
              }
            case AccessSubject.UserGroup(slug) =>
              UserGroupTable.findBySlug(connection, slug).map {
                case Some(_) => None
                case None => Some(s"Granted user group not found: ${slug.value}.")
              }
      }
    }

  def sanitizePolicy(request: CreateProblemRequest): CreateProblemRequest =
    request.copy(accessPolicy = sanitizePolicy(request.accessPolicy))

  def sanitizePolicy(request: UpdateProblemRequest): UpdateProblemRequest =
    request.copy(accessPolicy = sanitizePolicy(request.accessPolicy))

  def sanitizePolicy(accessPolicy: ResourceAccessPolicy): ResourceAccessPolicy =
    accessPolicy.copy(
      viewerGrants = accessPolicy.viewerGrants
        .distinctBy(subject => (AccessSubject.subjectKind(subject), AccessSubject.subjectKey(subject))),
      managerGrants = accessPolicy.managerGrants
        .distinctBy(subject => (AccessSubject.subjectKind(subject), AccessSubject.subjectKey(subject)))
    )
