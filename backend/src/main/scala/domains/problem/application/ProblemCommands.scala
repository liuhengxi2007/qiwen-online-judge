package domains.problem.application

import cats.effect.IO
import database.DatabaseSession
import domains.auth.model.AuthUser
import domains.problem.model.{CreateProblemRequest, ProblemDataFileListResponse, ProblemDataFilename, ProblemDetail, ProblemSummary, UpdateProblemDataRequest, UpdateProblemRequest}
import domains.problem.table.ProblemTable
import domains.problemset.model.ProblemSetSlug
import domains.problemset.table.ProblemSetTable
import domains.shared.access.{AccessPolicyEvaluator, AccessSubject, ResourceAccessPolicy, ResourceId, ResourceKind, ResourceViewerGrantTable}
import domains.shared.model.{PageRequest, PageResponse}
import domains.usergroup.table.UserGroupTable
import domains.auth.table.AuthUserTable
import domains.problem.application.ProblemDecisions.*

object ProblemCommands:

  enum CreateProblemResult:
    case Forbidden
    case ValidationFailed(message: String)
    case SlugAlreadyExists
    case SlugConflictsWithProblemSet
    case Created(problem: ProblemDetail)

  enum GetProblemResult:
    case NotFound
    case Forbidden
    case Found(problem: ProblemDetail)

  enum UpdateProblemResult:
    case Forbidden
    case ValidationFailed(message: String)
    case ProblemNotFound
    case Updated(problem: ProblemDetail)

  enum DeleteProblemResult:
    case Forbidden
    case ProblemNotFound
    case Deleted

  enum UpdateProblemDataResult:
    case Forbidden
    case ValidationFailed(message: String)
    case ProblemNotFound
    case Updated(problem: ProblemDetail)

  enum ListProblemDataResult:
    case Forbidden
    case ProblemNotFound
    case Listed(response: ProblemDataFileListResponse)

  enum DeleteProblemDataResult:
    case Forbidden
    case ProblemNotFound
    case DataFileNotFound
    case Deleted(problem: ProblemDetail)

  enum ClearProblemDataResult:
    case Forbidden
    case ProblemNotFound
    case Cleared(problem: ProblemDetail)

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
              result <- decideCreateProblem(existing, conflictingProblemSet, accessPolicyValidation) match
                case CreateProblemDecision.SlugAlreadyExists =>
                  IO.pure(CreateProblemResult.SlugAlreadyExists)
                case CreateProblemDecision.SlugConflictsWithProblemSet =>
                  IO.pure(CreateProblemResult.SlugConflictsWithProblemSet)
                case CreateProblemDecision.ValidationFailed(message) =>
                  IO.pure(CreateProblemResult.ValidationFailed(message))
                case CreateProblemDecision.Create =>
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
              result <- decideUpdateProblem(maybeProblem, accessPolicyValidation) match
                case UpdateProblemDecision.ProblemNotFound =>
                  IO.pure(UpdateProblemResult.ProblemNotFound)
                case UpdateProblemDecision.ValidationFailed(message) =>
                  IO.pure(UpdateProblemResult.ValidationFailed(message))
                case UpdateProblemDecision.Update(problem) =>
                  ProblemTable
                    .update(connection, problem.id, sanitizePolicy(problem.creatorUsername, validRequest))
                    .flatMap(_ =>
                      ProblemTable
                        .findBySlug(connection, problem.slug)
                        .map(updatedProblemOrError("Problem disappeared after update"))
                        .map(UpdateProblemResult.Updated(_))
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
              ResourceViewerGrantTable
                .deleteAllForResource(connection, ResourceKind.Problem, ResourceId(problem.id.value))
                .flatMap(_ => ProblemTable.delete(connection, problem.id))
                .as(DeleteProblemResult.Deleted)
        yield result
      }

  def updateProblemData(
    databaseSession: DatabaseSession,
    actor: AuthUser,
    problemSlug: domains.problem.model.ProblemSlug,
    request: UpdateProblemDataRequest
  ): IO[UpdateProblemDataResult] =
    if !ProblemPolicy.canEdit(actor) then
      IO.pure(UpdateProblemDataResult.Forbidden)
    else
      ProblemValidation.validateDataUpdate(request) match
        case Left(message) =>
          IO.pure(UpdateProblemDataResult.ValidationFailed(message))
        case Right(validRequest) =>
          validRequest.decodedBytes match
            case Left(message) =>
              IO.pure(UpdateProblemDataResult.ValidationFailed(message))
            case Right(decodedBytes) =>
              databaseSession.withTransactionConnection { connection =>
                for
                  maybeProblem <- ProblemTable.findBySlug(connection, problemSlug)
                  result <- decideUpdateProblemData(maybeProblem) match
                    case ProblemDataUpdateDecision.ProblemNotFound =>
                      IO.pure(UpdateProblemDataResult.ProblemNotFound)
                    case ProblemDataUpdateDecision.Update(problem) =>
                      for
                        snapshot <- ProblemDataStorage.snapshotDirectory(problem.slug)
                        result <- ProblemDataStorage
                          .writeFile(problem.slug, validRequest.filename, decodedBytes)
                          .flatMap(savedFilename =>
                            ProblemTable.updateData(connection, problem.id, savedFilename)
                              .flatMap(_ =>
                                ProblemTable
                                  .findBySlug(connection, problem.slug)
                                  .map(updatedProblemOrError("Problem disappeared after data update"))
                                  .map(UpdateProblemDataResult.Updated(_))
                              )
                          )
                          .handleErrorWith { error =>
                            ProblemDataStorage.restoreDirectory(problem.slug, snapshot) *> IO.raiseError(error)
                          }
                      yield result
                yield result
              }

  def listProblemData(
    databaseSession: DatabaseSession,
    actor: AuthUser,
    problemSlug: domains.problem.model.ProblemSlug
  ): IO[ListProblemDataResult] =
    if !ProblemPolicy.canEdit(actor) then
      IO.pure(ListProblemDataResult.Forbidden)
    else
      databaseSession.withTransactionConnection { connection =>
        ProblemTable.findBySlug(connection, problemSlug).flatMap {
          case None =>
            IO.pure(ListProblemDataResult.ProblemNotFound)
          case Some(problem) =>
            ProblemDataStorage
              .listFiles(problem.slug)
              .map(files => ListProblemDataResult.Listed(ProblemDataFileListResponse(files)))
        }
      }

  def deleteProblemData(
    databaseSession: DatabaseSession,
    actor: AuthUser,
    problemSlug: domains.problem.model.ProblemSlug,
    filename: ProblemDataFilename
  ): IO[DeleteProblemDataResult] =
    if !ProblemPolicy.canEdit(actor) then
      IO.pure(DeleteProblemDataResult.Forbidden)
    else
      databaseSession.withTransactionConnection { connection =>
        ProblemTable.findBySlug(connection, problemSlug).flatMap { maybeProblem =>
          decideDeleteProblemData(maybeProblem) match
            case ProblemDataDeletionDecision.ProblemNotFound =>
              IO.pure(DeleteProblemDataResult.ProblemNotFound)
            case ProblemDataDeletionDecision.Delete(problem) =>
              for
                snapshot <- ProblemDataStorage.snapshotDirectory(problem.slug)
                result <- ProblemDataStorage.deleteFile(problem.slug, filename).flatMap {
                  case false =>
                    IO.pure(DeleteProblemDataResult.DataFileNotFound)
                  case true =>
                    ProblemDataStorage
                      .listFiles(problem.slug)
                      .flatMap { files =>
                        ProblemTable.updateData(connection, problem.id, files.lastOption)
                      }
                      .flatMap(_ =>
                        ProblemTable
                          .findBySlug(connection, problem.slug)
                          .map(updatedProblemOrError("Problem disappeared after data deletion"))
                          .map(DeleteProblemDataResult.Deleted(_))
                      )
                      .handleErrorWith { error =>
                        ProblemDataStorage.restoreDirectory(problem.slug, snapshot) *> IO.raiseError(error)
                      }
                }
              yield result
        }
      }

  def clearProblemData(
    databaseSession: DatabaseSession,
    actor: AuthUser,
    problemSlug: domains.problem.model.ProblemSlug
  ): IO[ClearProblemDataResult] =
    if !ProblemPolicy.canEdit(actor) then
      IO.pure(ClearProblemDataResult.Forbidden)
    else
      databaseSession.withTransactionConnection { connection =>
        ProblemTable.findBySlug(connection, problemSlug).flatMap { maybeProblem =>
          decideClearProblemData(maybeProblem) match
            case ProblemDataClearDecision.ProblemNotFound =>
              IO.pure(ClearProblemDataResult.ProblemNotFound)
            case ProblemDataClearDecision.Clear(problem) =>
              for
                snapshot <- ProblemDataStorage.snapshotDirectory(problem.slug)
                result <- ProblemDataStorage
                  .deleteAllFiles(problem.slug)
                  .flatMap(_ => ProblemTable.updateData(connection, problem.id, None))
                  .flatMap(_ =>
                    ProblemTable
                      .findBySlug(connection, problem.slug)
                      .map(updatedProblemOrError("Problem disappeared after clearing data"))
                      .map(ClearProblemDataResult.Cleared(_))
                  )
                  .handleErrorWith { error =>
                    ProblemDataStorage.restoreDirectory(problem.slug, snapshot) *> IO.raiseError(error)
                  }
              yield result
        }
      }

  private def findConflictingProblemSet(
    connection: java.sql.Connection,
    rawSlug: String
  ): IO[Option[domains.problemset.model.ProblemSet]] =
    ProblemSetSlug.parse(rawSlug) match
      case Left(_) => IO.pure(None)
      case Right(slug) => ProblemSetTable.findBySlug(connection, slug)

  private def updatedProblemOrError(message: String)(maybeProblem: Option[ProblemDetail]): ProblemDetail =
    maybeProblem.getOrElse(throw new IllegalStateException(message))

  private def canViewProblem(
    connection: java.sql.Connection,
    actor: AuthUser,
    problem: ProblemDetail
  ): IO[Boolean] =
    UserGroupTable.listGroupSlugsForMember(connection, actor.username).flatMap { viewerGroupSlugs =>
      val canViewDirectly = AccessPolicyEvaluator.canView(
        policy = problem.accessPolicy,
        viewerUsername = actor.username,
        viewerGroupSlugs = viewerGroupSlugs,
        isOwner = problem.creatorUsername.value == actor.username.value,
        hasGlobalOverride = ProblemPolicy.hasGlobalViewOverride(actor)
      )

      if canViewDirectly then
        IO.pure(true)
      else
        ProblemTable.hasVisibleContainingProblemSet(connection, actor, problem.id)
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
