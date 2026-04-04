package domains.problem.application

import cats.effect.IO
import database.DatabaseSession
import domains.auth.model.AuthUser
import domains.problem.model.{CreateProblemRequest, Problem, ProblemSummary, UpdateProblemRequest}
import domains.problem.table.ProblemTable
import domains.problemset.model.ProblemSetSlug
import domains.problemset.table.ProblemSetTable
import domains.shared.access.{AccessSubject, BaseAccess, ResourceAccessPolicy, ResourceViewerGrantTable}
import domains.shared.model.{PageRequest, PageResponse}
import domains.usergroup.table.UserGroupTable
import domains.auth.table.AuthUserTable

object ProblemCommands:

  enum CreateProblemResult:
    case Forbidden
    case ValidationFailed(message: String)
    case SlugAlreadyExists
    case SlugConflictsWithProblemSet
    case Created(problem: Problem)

  enum GetProblemResult:
    case NotFound
    case Forbidden
    case Found(problem: Problem)

  enum UpdateProblemResult:
    case Forbidden
    case ValidationFailed(message: String)
    case ProblemNotFound
    case Updated(problem: Problem)

  enum DeleteProblemResult:
    case Forbidden
    case ProblemNotFound
    case Deleted

  def listProblems(
    databaseSession: DatabaseSession,
    actor: AuthUser,
    pageRequest: PageRequest
  ): IO[PageResponse[ProblemSummary]] =
    val normalizedPageRequest = pageRequest.normalized
    databaseSession.withTransactionConnection { connection =>
      ProblemTable.listVisibleTo(connection, actor, normalizedPageRequest.page, normalizedPageRequest.pageSize)
    }

  def createProblem(
    databaseSession: DatabaseSession,
    actor: AuthUser,
    request: CreateProblemRequest
  ): IO[CreateProblemResult] =
    if !ProblemPolicy.canCreate(actor) then
      IO.pure(CreateProblemResult.Forbidden)
    else
      ProblemValidation.validateCreate(request) match
        case Left(message) =>
          IO.pure(CreateProblemResult.ValidationFailed(message))
        case Right(validRequest) =>
          databaseSession.withTransactionConnection { connection =>
            for
              existing <- ProblemTable.findBySlug(connection, validRequest.slug)
              conflictingProblemSet <- findConflictingProblemSet(connection, validRequest.slug.value)
              accessPolicyValidation <- validateAccessPolicySubjects(connection, validRequest.accessPolicy)
              result <- existing match
                case Some(_) =>
                  IO.pure(CreateProblemResult.SlugAlreadyExists)
                case None if conflictingProblemSet.nonEmpty =>
                  IO.pure(CreateProblemResult.SlugConflictsWithProblemSet)
                case None if accessPolicyValidation.nonEmpty =>
                  IO.pure(CreateProblemResult.ValidationFailed(accessPolicyValidation.get))
                case None =>
                  ProblemTable
                    .insert(connection, actor.username, sanitizePolicy(actor.username, validRequest))
                    .map(problem => CreateProblemResult.Created(problem))
            yield result
          }

  def getProblemBySlug(
    databaseSession: DatabaseSession,
    actor: AuthUser,
    slug: domains.problem.model.ProblemSlug
  ): IO[GetProblemResult] =
    databaseSession.withTransactionConnection { connection =>
      ProblemTable.findBySlug(connection, slug).flatMap {
        case Some(problem) =>
          canViewProblem(connection, actor, problem).map {
            case true => GetProblemResult.Found(problem)
            case false => GetProblemResult.Forbidden
          }
        case None =>
          IO.pure(GetProblemResult.NotFound)
      }
    }

  def updateProblem(
    databaseSession: DatabaseSession,
    actor: AuthUser,
    problemSlug: domains.problem.model.ProblemSlug,
    request: UpdateProblemRequest
  ): IO[UpdateProblemResult] =
    if !ProblemPolicy.canEdit(actor) then
      IO.pure(UpdateProblemResult.Forbidden)
    else
      ProblemValidation.validateUpdate(request) match
        case Left(message) =>
          IO.pure(UpdateProblemResult.ValidationFailed(message))
        case Right(validRequest) =>
          databaseSession.withTransactionConnection { connection =>
            for
              maybeProblem <- ProblemTable.findBySlug(connection, problemSlug)
              accessPolicyValidation <- validateAccessPolicySubjects(connection, validRequest.accessPolicy)
              result <- maybeProblem match
                case None =>
                  IO.pure(UpdateProblemResult.ProblemNotFound)
                case Some(_) if accessPolicyValidation.nonEmpty =>
                  IO.pure(UpdateProblemResult.ValidationFailed(accessPolicyValidation.get))
                case Some(problem) =>
                  ProblemTable
                    .update(connection, problem.id, sanitizePolicy(problem.ownerUsername, validRequest))
                    .flatMap(_ =>
                      ProblemTable.findBySlug(connection, problem.slug).map {
                        case Some(updatedProblem) => UpdateProblemResult.Updated(updatedProblem)
                        case None => throw new IllegalStateException("Problem disappeared after update")
                      }
                    )
            yield result
          }

  def deleteProblem(
    databaseSession: DatabaseSession,
    actor: AuthUser,
    problemSlug: domains.problem.model.ProblemSlug
  ): IO[DeleteProblemResult] =
    if !ProblemPolicy.canDelete(actor) then
      IO.pure(DeleteProblemResult.Forbidden)
    else
      databaseSession.withTransactionConnection { connection =>
        for
          maybeProblem <- ProblemTable.findBySlug(connection, problemSlug)
          result <- maybeProblem match
            case None =>
              IO.pure(DeleteProblemResult.ProblemNotFound)
            case Some(problem) =>
              ProblemTable.delete(connection, problem.id).as(DeleteProblemResult.Deleted)
        yield result
      }

  private def findConflictingProblemSet(
    connection: java.sql.Connection,
    rawSlug: String
  ): IO[Option[domains.problemset.model.ProblemSet]] =
    ProblemSetSlug.parse(rawSlug) match
      case Left(_) => IO.pure(None)
      case Right(slug) => ProblemSetTable.findBySlug(connection, slug)

  private def canViewProblem(
    connection: java.sql.Connection,
    actor: AuthUser,
    problem: Problem
  ): IO[Boolean] =
    if problem.ownerUsername.value == actor.username.value || ProblemPolicy.hasGlobalViewOverride(actor) then
      IO.pure(true)
    else if problem.accessPolicy.baseAccess == BaseAccess.Public then
      IO.pure(true)
    else
      for
        hasDirectGrant <- ResourceViewerGrantTable.hasDirectUserGrant(
          connection,
          domains.shared.access.ResourceKind.Problem,
          domains.shared.access.ResourceId(problem.id.value),
          actor.username
        )
        hasGroupGrant <- if hasDirectGrant then IO.pure(false)
        else
          ResourceViewerGrantTable.hasAnyGrantedUserGroup(
            connection,
            domains.shared.access.ResourceKind.Problem,
            domains.shared.access.ResourceId(problem.id.value),
            actor.username
          )
      yield hasDirectGrant || hasGroupGrant

  private def validateAccessPolicySubjects(
    connection: java.sql.Connection,
    policy: ResourceAccessPolicy
  ): IO[Option[String]] =
    policy.viewerGrants.foldLeft(IO.pure(Option.empty[String])) { (accIO, subject) =>
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

  private def sanitizePolicy(
    ownerUsername: domains.auth.model.Username,
    request: CreateProblemRequest
  ): CreateProblemRequest =
    request.copy(accessPolicy = sanitizePolicy(ownerUsername, request.accessPolicy))

  private def sanitizePolicy(
    ownerUsername: domains.auth.model.Username,
    request: UpdateProblemRequest
  ): UpdateProblemRequest =
    request.copy(accessPolicy = sanitizePolicy(ownerUsername, request.accessPolicy))

  private def sanitizePolicy(
    ownerUsername: domains.auth.model.Username,
    accessPolicy: ResourceAccessPolicy
  ): ResourceAccessPolicy =
    accessPolicy.copy(
      viewerGrants = accessPolicy.viewerGrants
        .distinctBy(subject => (AccessSubject.subjectKind(subject), AccessSubject.subjectKey(subject)))
        .filter {
          case AccessSubject.User(username) => username.value != ownerUsername.value
          case AccessSubject.UserGroup(_) => true
        }
    )
