package domains.problemset.application

import cats.effect.IO
import database.DatabaseSession
import domains.auth.model.AuthUser
import domains.auth.table.AuthUserTable
import domains.problem.table.ProblemTable
import domains.problemset.model.{AddProblemToProblemSetRequest, CreateProblemSetRequest, ProblemSet, ProblemSetSummary, UpdateProblemSetRequest}
import domains.problemset.table.ProblemSetTable
import domains.shared.access.{AccessPolicyEvaluator, AccessSubject, ResourceAccessPolicy, ResourceId, ResourceKind, ResourceViewerGrantTable}
import domains.shared.model.{PageRequest, PageResponse}
import domains.usergroup.table.UserGroupTable

object ProblemSetCommands:

  enum CreateProblemSetResult:
    case Forbidden
    case ValidationFailed(message: String)
    case SlugAlreadyExists
    case SlugConflictsWithProblem
    case Created(problemSet: ProblemSet)

  enum AddProblemResult:
    case Forbidden
    case ValidationFailed(message: String)
    case ProblemSetNotFound
    case ProblemNotFound
    case ProblemAlreadyLinked
    case Linked(problemSet: ProblemSet)

  enum GetProblemSetResult:
    case NotFound
    case Forbidden
    case Found(problemSet: ProblemSet)

  enum UpdateProblemSetResult:
    case Forbidden
    case ValidationFailed(message: String)
    case ProblemSetNotFound
    case Updated(problemSet: ProblemSet)

  enum DeleteProblemSetResult:
    case Forbidden
    case ProblemSetNotFound
    case Deleted

  enum RemoveProblemResult:
    case Forbidden
    case ProblemSetNotFound
    case ProblemNotFound
    case ProblemNotLinked
    case Removed(problemSet: ProblemSet)

  def listProblemSets(
    databaseSession: DatabaseSession,
    actor: AuthUser,
    pageRequest: PageRequest
  ): IO[PageResponse[ProblemSetSummary]] =
    val normalizedPageRequest = pageRequest.normalized
    databaseSession.withTransactionConnection { connection =>
      ProblemSetTable.listVisibleTo(connection, actor, normalizedPageRequest.page, normalizedPageRequest.pageSize)
    }

  def createProblemSet(
    databaseSession: DatabaseSession,
    actor: AuthUser,
    request: CreateProblemSetRequest
  ): IO[CreateProblemSetResult] =
    if !ProblemSetPolicy.canCreate(actor) then
      IO.pure(CreateProblemSetResult.Forbidden)
    else
      ProblemSetValidation.validateCreate(request) match
        case Left(message) =>
          IO.pure(CreateProblemSetResult.ValidationFailed(message))
        case Right(validRequest) =>
          databaseSession.withTransactionConnection { connection =>
            for
              existing <- ProblemSetTable.findBySlug(connection, validRequest.slug)
              conflictingProblem <- ProblemTable.findBySlug(connection, domains.problem.model.ProblemSlug.unsafe(validRequest.slug.value))
              accessPolicyValidation <- validateAccessPolicySubjects(connection, validRequest.accessPolicy)
              result <- existing match
                case Some(_) =>
                  IO.pure(CreateProblemSetResult.SlugAlreadyExists)
                case None if conflictingProblem.nonEmpty =>
                  IO.pure(CreateProblemSetResult.SlugConflictsWithProblem)
                case None if accessPolicyValidation.nonEmpty =>
                  IO.pure(CreateProblemSetResult.ValidationFailed(accessPolicyValidation.get))
                case None =>
                  ProblemSetTable
                    .insert(connection, actor.username, sanitizePolicy(actor.username, validRequest))
                    .map(problemSet => CreateProblemSetResult.Created(problemSet))
            yield result
          }

  def getProblemSetBySlug(
    databaseSession: DatabaseSession,
    actor: AuthUser,
    slug: domains.problemset.model.ProblemSetSlug
  ): IO[GetProblemSetResult] =
    databaseSession.withTransactionConnection { connection =>
      ProblemSetTable.findBySlug(connection, slug).flatMap {
        case Some(problemSet) =>
          canViewProblemSet(connection, actor, problemSet).map {
            case true => GetProblemSetResult.Found(problemSet)
            case false => GetProblemSetResult.Forbidden
          }
        case None =>
          IO.pure(GetProblemSetResult.NotFound)
      }
    }

  def addProblemToProblemSet(
    databaseSession: DatabaseSession,
    actor: AuthUser,
    problemSetSlug: domains.problemset.model.ProblemSetSlug,
    request: AddProblemToProblemSetRequest
  ): IO[AddProblemResult] =
    if !ProblemSetPolicy.canManageProblems(actor) then
      IO.pure(AddProblemResult.Forbidden)
    else
      ProblemSetValidation.validateAddProblem(request) match
        case Left(message) =>
          IO.pure(AddProblemResult.ValidationFailed(message))
        case Right(validRequest) =>
          databaseSession.withTransactionConnection { connection =>
            for
              maybeProblemSet <- ProblemSetTable.findBySlug(connection, problemSetSlug)
              result <- maybeProblemSet match
                case None =>
                  IO.pure(AddProblemResult.ProblemSetNotFound)
                case Some(problemSet) =>
                  for
                    maybeProblem <- ProblemTable.findBySlug(connection, validRequest.problemSlug)
                    linkedResult <- maybeProblem match
                      case None =>
                        IO.pure(AddProblemResult.ProblemNotFound)
                      case Some(problem) =>
                        ProblemSetTable
                          .addProblem(connection, problemSet.id, problem.id)
                          .flatMap {
                            case ProblemSetTable.AddProblemTableResult.AlreadyLinked =>
                              IO.pure(AddProblemResult.ProblemAlreadyLinked)
                            case ProblemSetTable.AddProblemTableResult.Linked =>
                              ProblemSetTable
                                .findBySlug(connection, problemSet.slug)
                                .map {
                                  case Some(updatedProblemSet) => AddProblemResult.Linked(updatedProblemSet)
                                  case None => throw new IllegalStateException("Problem set disappeared after problem link")
                                }
                          }
                  yield linkedResult
            yield result
          }

  def updateProblemSet(
    databaseSession: DatabaseSession,
    actor: AuthUser,
    problemSetSlug: domains.problemset.model.ProblemSetSlug,
    request: UpdateProblemSetRequest
  ): IO[UpdateProblemSetResult] =
    if !ProblemSetPolicy.canEdit(actor) then
      IO.pure(UpdateProblemSetResult.Forbidden)
    else
      ProblemSetValidation.validateUpdate(request) match
        case Left(message) =>
          IO.pure(UpdateProblemSetResult.ValidationFailed(message))
        case Right(validRequest) =>
          databaseSession.withTransactionConnection { connection =>
            for
              maybeProblemSet <- ProblemSetTable.findBySlug(connection, problemSetSlug)
              accessPolicyValidation <- validateAccessPolicySubjects(connection, validRequest.accessPolicy)
              result <- maybeProblemSet match
                case None =>
                  IO.pure(UpdateProblemSetResult.ProblemSetNotFound)
                case Some(_) if accessPolicyValidation.nonEmpty =>
                  IO.pure(UpdateProblemSetResult.ValidationFailed(accessPolicyValidation.get))
                case Some(problemSet) =>
                  ProblemSetTable
                    .update(connection, problemSet.id, sanitizePolicy(problemSet.ownerUsername, validRequest))
                    .flatMap(_ =>
                      ProblemSetTable.findBySlug(connection, problemSet.slug).map {
                        case Some(updatedProblemSet) => UpdateProblemSetResult.Updated(updatedProblemSet)
                        case None => throw new IllegalStateException("Problem set disappeared after update")
                      }
                    )
            yield result
          }

  def deleteProblemSet(
    databaseSession: DatabaseSession,
    actor: AuthUser,
    problemSetSlug: domains.problemset.model.ProblemSetSlug
  ): IO[DeleteProblemSetResult] =
    if !ProblemSetPolicy.canDelete(actor) then
      IO.pure(DeleteProblemSetResult.Forbidden)
    else
      databaseSession.withTransactionConnection { connection =>
        for
          maybeProblemSet <- ProblemSetTable.findBySlug(connection, problemSetSlug)
          result <- maybeProblemSet match
            case None =>
              IO.pure(DeleteProblemSetResult.ProblemSetNotFound)
            case Some(problemSet) =>
              ResourceViewerGrantTable
                .deleteAllForResource(connection, ResourceKind.ProblemSet, ResourceId(problemSet.id.value))
                .flatMap(_ => ProblemSetTable.delete(connection, problemSet.id))
                .as(DeleteProblemSetResult.Deleted)
        yield result
      }

  private def canViewProblemSet(
    connection: java.sql.Connection,
    actor: AuthUser,
    problemSet: ProblemSet
  ): IO[Boolean] =
    UserGroupTable.listGroupSlugsForMember(connection, actor.username).map { viewerGroupSlugs =>
      AccessPolicyEvaluator.canView(
        policy = problemSet.accessPolicy,
        viewerUsername = actor.username,
        viewerGroupSlugs = viewerGroupSlugs,
        isOwner = problemSet.ownerUsername.value == actor.username.value,
        hasGlobalOverride = ProblemSetPolicy.hasGlobalViewOverride(actor)
      )
    }

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
    request: CreateProblemSetRequest
  ): CreateProblemSetRequest =
    request.copy(accessPolicy = sanitizePolicy(ownerUsername, request.accessPolicy))

  private def sanitizePolicy(
    ownerUsername: domains.auth.model.Username,
    request: UpdateProblemSetRequest
  ): UpdateProblemSetRequest =
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

  def removeProblemFromProblemSet(
    databaseSession: DatabaseSession,
    actor: AuthUser,
    problemSetSlug: domains.problemset.model.ProblemSetSlug,
    problemSlug: domains.problem.model.ProblemSlug
  ): IO[RemoveProblemResult] =
    if !ProblemSetPolicy.canManageProblems(actor) then
      IO.pure(RemoveProblemResult.Forbidden)
    else
      databaseSession.withTransactionConnection { connection =>
        for
          maybeProblemSet <- ProblemSetTable.findBySlug(connection, problemSetSlug)
          result <- maybeProblemSet match
            case None =>
              IO.pure(RemoveProblemResult.ProblemSetNotFound)
            case Some(problemSet) =>
              for
                maybeProblem <- ProblemTable.findBySlug(connection, problemSlug)
                removeResult <- maybeProblem match
                  case None =>
                    IO.pure(RemoveProblemResult.ProblemNotFound)
                  case Some(problem) =>
                    ProblemSetTable.removeProblem(connection, problemSet.id, problem.id).flatMap {
                      case ProblemSetTable.RemoveProblemTableResult.NotLinked =>
                        IO.pure(RemoveProblemResult.ProblemNotLinked)
                      case ProblemSetTable.RemoveProblemTableResult.Removed =>
                        ProblemSetTable.findBySlug(connection, problemSet.slug).map {
                          case Some(updatedProblemSet) => RemoveProblemResult.Removed(updatedProblemSet)
                          case None => throw new IllegalStateException("Problem set disappeared after problem removal")
                        }
                    }
              yield removeResult
        yield result
      }
