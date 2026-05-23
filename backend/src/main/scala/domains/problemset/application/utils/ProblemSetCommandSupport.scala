package domains.problemset.application.utils



import domains.problemset.application.{ProblemSetAccessDecision, ProblemSetAccessFacts, ProblemSetPolicy}
import cats.effect.IO
import domains.auth.application.AuthCommands
import domains.auth.model.AuthUser
import domains.problemset.application.input.{CreateProblemSetRequest, UpdateProblemSetRequest}
import domains.problemset.model.{ProblemSet}
import shared.access.{AccessSubject, ResourceAccessFacts, ResourceAccessPolicy}
import domains.usergroup.application.UserGroupCommands

object ProblemSetCommandSupport:

  def canViewProblemSet(
    connection: java.sql.Connection,
    actor: AuthUser,
    problemSet: ProblemSet
  ): IO[Boolean] =
    UserGroupCommands.accessActorGroupSlugs(connection, actor.username).map { viewerGroupSlugs =>
      ProblemSetAccessDecision
        .evaluate(
          ProblemSetAccessFacts(
            resourceAccess = ResourceAccessFacts(
              policy = problemSet.accessPolicy,
              actorUsername = actor.username,
              actorGroupSlugs = viewerGroupSlugs,
              hasGlobalViewOverride = ProblemSetPolicy.hasGlobalViewOverride(actor),
              hasGlobalManageOverride = ProblemSetPolicy.hasGlobalViewOverride(actor)
            )
          )
        )
        .canView
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

  def updatedProblemSetOrError(message: String)(maybeProblemSet: Option[ProblemSet]): ProblemSet =
    maybeProblemSet.getOrElse(throw new IllegalStateException(message))
