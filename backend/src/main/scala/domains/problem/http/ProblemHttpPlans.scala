package domains.problem.http

import cats.effect.IO
import database.DatabaseSession
import domains.auth.model.AuthUser
import domains.problem.application.ProblemCommands
import domains.problem.model.{CreateProblemRequest, DeleteProblemDataPathRequest, ProblemDataFilename, ProblemDataPath, ProblemListRequest, ProblemSearchQuery, ProblemSlug, ProblemSuggestion, UpdateProblemDataRequest, UpdateProblemRequest}
import domains.shared.http.{PlainAuthenticatedHttpPlan, TransactionAuthenticatedHttpPlan}
import io.circe.syntax.*

import java.sql.Connection

object ProblemHttpPlans:

  final case class DownloadProblemDataOutput(
    problemSlug: ProblemSlug,
    filename: ProblemDataFilename,
    authorization: ProblemCommands.AuthorizeProblemDataDownloadResult
  )

  case object ListProblems extends PlainAuthenticatedHttpPlan[ProblemListRequest, domains.shared.model.PageResponse[domains.problem.model.ProblemSummary]]:

    override val name: String = "ListProblems"

    override def execute(
      databaseSession: DatabaseSession,
      actor: AuthUser,
      input: ProblemListRequest
    ): IO[domains.shared.model.PageResponse[domains.problem.model.ProblemSummary]] =
      ProblemCommands.listProblems(databaseSession, actor, input)

  case object CreateProblem extends TransactionAuthenticatedHttpPlan[CreateProblemRequest, ProblemCommands.CreateProblemResult]:

    override val name: String = "CreateProblem"

    override def execute(
      connection: Connection,
      actor: AuthUser,
      input: CreateProblemRequest
    ): IO[ProblemCommands.CreateProblemResult] =
      ProblemCommands
        .createProblem(connection, actor, input)

  case object ListProblemSuggestions extends PlainAuthenticatedHttpPlan[ProblemSearchQuery, List[ProblemSuggestion]]:

    override val name: String = "ListProblemSuggestions"

    override def execute(
      databaseSession: DatabaseSession,
      actor: AuthUser,
      input: ProblemSearchQuery
    ): IO[List[ProblemSuggestion]] =
      ProblemCommands.listProblemSuggestions(databaseSession, actor, input)

  case object GetProblem extends PlainAuthenticatedHttpPlan[ProblemSlug, ProblemCommands.GetProblemResult]:

    override val name: String = "GetProblem"

    override def execute(
      databaseSession: DatabaseSession,
      actor: AuthUser,
      input: ProblemSlug
    ): IO[ProblemCommands.GetProblemResult] =
      ProblemCommands
        .getProblemBySlug(databaseSession, actor, input)

  case object ListProblemData extends PlainAuthenticatedHttpPlan[ProblemSlug, ProblemCommands.ListProblemDataResult]:

    override val name: String = "ListProblemData"

    override def execute(
      databaseSession: DatabaseSession,
      actor: AuthUser,
      input: ProblemSlug
    ): IO[ProblemCommands.ListProblemDataResult] =
      ProblemCommands
        .listProblemData(databaseSession, actor, input)

  case object ListProblemDataTree extends PlainAuthenticatedHttpPlan[ProblemSlug, ProblemCommands.ListProblemDataTreeResult]:

    override val name: String = "ListProblemDataTree"

    override def execute(
      databaseSession: DatabaseSession,
      actor: AuthUser,
      input: ProblemSlug
    ): IO[ProblemCommands.ListProblemDataTreeResult] =
      ProblemCommands
        .listProblemDataTree(databaseSession, actor, input)

  case object DownloadProblemData extends PlainAuthenticatedHttpPlan[(ProblemSlug, ProblemDataFilename), DownloadProblemDataOutput]:

    override val name: String = "DownloadProblemData"

    override def execute(
      databaseSession: DatabaseSession,
      actor: AuthUser,
      input: (ProblemSlug, ProblemDataFilename)
    ): IO[DownloadProblemDataOutput] =
      val (problemSlug, filename) = input
      ProblemCommands
        .authorizeProblemDataDownload(databaseSession, actor, problemSlug)
        .map(authorization => DownloadProblemDataOutput(problemSlug, filename, authorization))

  case object DeleteProblemData extends TransactionAuthenticatedHttpPlan[(ProblemSlug, ProblemDataFilename), ProblemCommands.DeleteProblemDataResult]:

    override val name: String = "DeleteProblemData"

    override def execute(
      connection: Connection,
      actor: AuthUser,
      input: (ProblemSlug, ProblemDataFilename)
    ): IO[ProblemCommands.DeleteProblemDataResult] =
      val (problemSlug, filename) = input
      ProblemCommands
        .deleteProblemData(connection, actor, problemSlug, filename)

  case object DeleteProblemDataPath extends TransactionAuthenticatedHttpPlan[(ProblemSlug, DeleteProblemDataPathRequest), ProblemCommands.DeleteProblemDataResult]:

    override val name: String = "DeleteProblemDataPath"

    override def execute(
      connection: Connection,
      actor: AuthUser,
      input: (ProblemSlug, DeleteProblemDataPathRequest)
    ): IO[ProblemCommands.DeleteProblemDataResult] =
      val (problemSlug, request) = input
      ProblemCommands
        .deleteProblemDataPath(connection, actor, problemSlug, request.path)

  case object ClearProblemData extends TransactionAuthenticatedHttpPlan[ProblemSlug, ProblemCommands.ClearProblemDataResult]:

    override val name: String = "ClearProblemData"

    override def execute(
      connection: Connection,
      actor: AuthUser,
      input: ProblemSlug
    ): IO[ProblemCommands.ClearProblemDataResult] =
      ProblemCommands
        .clearProblemData(connection, actor, input)

  case object UpdateProblem extends TransactionAuthenticatedHttpPlan[(ProblemSlug, UpdateProblemRequest), ProblemCommands.UpdateProblemResult]:

    override val name: String = "UpdateProblem"

    override def execute(
      connection: Connection,
      actor: AuthUser,
      input: (ProblemSlug, UpdateProblemRequest)
    ): IO[ProblemCommands.UpdateProblemResult] =
      val (problemSlug, request) = input
      ProblemCommands
        .updateProblem(connection, actor, problemSlug, request)

  case object UpdateProblemData extends TransactionAuthenticatedHttpPlan[(ProblemSlug, UpdateProblemDataRequest), ProblemCommands.UpdateProblemDataResult]:

    override val name: String = "UpdateProblemData"

    override def execute(
      connection: Connection,
      actor: AuthUser,
      input: (ProblemSlug, UpdateProblemDataRequest)
    ): IO[ProblemCommands.UpdateProblemDataResult] =
      val (problemSlug, request) = input
      ProblemCommands
        .updateProblemData(connection, actor, problemSlug, request)

  case object DeleteProblem extends TransactionAuthenticatedHttpPlan[ProblemSlug, ProblemCommands.DeleteProblemResult]:

    override val name: String = "DeleteProblem"

    override def execute(
      connection: Connection,
      actor: AuthUser,
      input: ProblemSlug
    ): IO[ProblemCommands.DeleteProblemResult] =
      ProblemCommands
        .deleteProblem(connection, actor, input)
