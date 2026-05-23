package domains.problem.application.utils



import domains.problem.application.{ProblemAccessDecision, ProblemAccessFacts, ProblemPolicy}
import cats.effect.IO
import domains.auth.application.AuthCommands
import domains.auth.model.AuthUser
import domains.problem.application.input.{CreateProblemRequest, UpdateProblemRequest}
import domains.problem.application.output.{ProblemDetail}
import domains.problemset.application.ProblemSetCommands
import shared.access.{AccessSubject, ResourceAccessFacts, ResourceAccessPolicy}
import domains.usergroup.application.UserGroupCommands

object ProblemCommandSupport:

  final case class ProblemPermissionEvaluation(
    canView: Boolean,
    canManage: Boolean
  )

  def hasConflictingProblemSetSlug(
    connection: java.sql.Connection,
    rawSlug: String
  ): IO[Boolean] =
    ProblemSetCommands.problemSetSlugConflictsWith(connection, rawSlug)

  def updatedProblemOrError(message: String)(maybeProblem: Option[ProblemDetail]): ProblemDetail =
    maybeProblem.getOrElse(throw new IllegalStateException(message))

  def enrichProblemPermissions(
    connection: java.sql.Connection,
    actor: AuthUser,
    problem: ProblemDetail
  ): IO[Option[ProblemDetail]] =
    evaluateProblemPermissions(connection, actor, problem).map { decision =>
      if decision.canView then Some(problem.copy(canManage = decision.canManage)) else None
    }

  def evaluateProblemPermissions(
    connection: java.sql.Connection,
    actor: AuthUser,
    problem: ProblemDetail
  ): IO[ProblemPermissionEvaluation] =
    UserGroupCommands.accessActorGroupSlugs(connection, actor.username).flatMap { actorGroupSlugs =>
      val resourceAccessFacts = ResourceAccessFacts(
        policy = problem.accessPolicy,
        actorUsername = actor.username,
        actorGroupSlugs = actorGroupSlugs,
        hasGlobalViewOverride = ProblemPolicy.hasGlobalViewOverride(actor),
        hasGlobalManageOverride = ProblemPolicy.hasGlobalManageOverride(actor)
      )

      domains.problem.table.problem.ProblemTable.hasVisibleContainingProblemSet(connection, actor, problem.id).map { hasVisibleContainingProblemSet =>
        val decision = ProblemAccessDecision.evaluate(
          ProblemAccessFacts(
            resourceAccess = resourceAccessFacts,
            hasVisibleContainingProblemSet = hasVisibleContainingProblemSet
          )
        )

        ProblemPermissionEvaluation(
          canView = decision.canView,
          canManage = decision.canManage
        )
      }
    }

  def canManageProblem(
    connection: java.sql.Connection,
    actor: AuthUser,
    problem: ProblemDetail
  ): IO[Boolean] =
    UserGroupCommands.accessActorGroupSlugs(connection, actor.username).map { actorGroupSlugs =>
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
              AuthCommands.accessPolicyUserExists(connection, username).map(exists =>
                if exists then None else Some(s"Granted user not found: ${username.value}.")
              )
            case AccessSubject.UserGroup(slug) =>
              UserGroupCommands.accessPolicyUserGroupExists(connection, slug).map(exists =>
                if exists then None else Some(s"Granted user group not found: ${slug.value}.")
              )
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
