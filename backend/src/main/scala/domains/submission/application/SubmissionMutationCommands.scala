package domains.submission.application

import cats.effect.IO
import database.DatabaseSession
import domains.auth.model.AuthUser
import domains.problem.table.ProblemTable
import domains.submission.model.CreateSubmissionRequest
import domains.submission.table.SubmissionTable
import domains.submission.application.SubmissionCommandResults.*
import domains.submission.application.SubmissionCommandSupport.*

import java.sql.Connection

object SubmissionMutationCommands:

  def createSubmission(
    databaseSession: DatabaseSession,
    actor: AuthUser,
    request: CreateSubmissionRequest
  ): IO[CreateSubmissionResult] =
    databaseSession.withTransactionConnection(connection =>
      createSubmission(connection, actor, request)
    )

  def createSubmission(
    connection: Connection,
    actor: AuthUser,
    request: CreateSubmissionRequest
  ): IO[CreateSubmissionResult] =
    SubmissionValidation.validateCreate(request) match
      case Left(message) =>
        IO.pure(CreateSubmissionResult.ValidationFailed(message))
      case Right(validRequest) =>
        ProblemTable.findBySlug(connection, validRequest.problemSlug).flatMap {
          case None =>
            IO.pure(CreateSubmissionResult.ProblemNotFound)
          case Some(problem) =>
            canSubmitToProblem(connection, actor, problem).flatMap {
              case false =>
                IO.pure(CreateSubmissionResult.Forbidden)
              case true =>
                SubmissionTable
                  .insert(
                    connection = connection,
                    problemId = problem.id,
                    problemSlug = problem.slug,
                    problemTitle = problem.title,
                    submitterUsername = actor.username,
                    language = validRequest.language,
                    sourceCode = validRequest.sourceCode,
                  )
                  .map(submission => CreateSubmissionResult.Created(submission))
            }
        }
