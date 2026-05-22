package domains.problem.http



import cats.effect.IO
import database.DatabaseSession
import domains.auth.model.AuthUser
import domains.problem.application.{ProblemCommands, ProblemDataStorage}
import domains.problem.application.input.{CreateProblemRequest, DeleteProblemDataPathRequest, ProblemListRequest, ProblemSearchQuery, UpdateProblemRequest}
import domains.problem.model.{ProblemDataFilename, ProblemDataPath, ProblemSlug}
import domains.problem.application.output.{ProblemSuggestion}
import shared.http.{PlainAuthenticatedHttpPlan, TransactionAuthenticatedHttpPlan}

import java.sql.Connection

object ProblemHttpPlans:

  final case class SetProblemReadyRequest(ready: Boolean)

  final case class DownloadProblemDataOutput(
    problemSlug: ProblemSlug,
    filename: ProblemDataFilename,
    authorization: ProblemCommands.AuthorizeProblemDataDownloadResult
  )

  case object ListProblems extends PlainAuthenticatedHttpPlan[ProblemListRequest, shared.model.PageResponse[domains.problem.application.output.ProblemSummary]]:

    override val name: String = "ListProblems"

    override def execute(
      databaseSession: DatabaseSession,
      actor: AuthUser,
      input: ProblemListRequest
    ): IO[shared.model.PageResponse[domains.problem.application.output.ProblemSummary]] =
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

  final class ListProblemDataPlan(problemDataStorage: ProblemDataStorage)
      extends PlainAuthenticatedHttpPlan[ProblemSlug, ProblemCommands.ListProblemDataResult]:

    override val name: String = "ListProblemData"

    override def execute(
      databaseSession: DatabaseSession,
      actor: AuthUser,
      input: ProblemSlug
    ): IO[ProblemCommands.ListProblemDataResult] =
      ProblemCommands
        .listProblemData(problemDataStorage, databaseSession, actor, input)

  case object ListProblemDataTree extends PlainAuthenticatedHttpPlan[ProblemSlug, ProblemCommands.ListProblemDataTreeResult]:

    override val name: String = "ListProblemDataTree"

    override def execute(
      databaseSession: DatabaseSession,
      actor: AuthUser,
      input: ProblemSlug
    ): IO[ProblemCommands.ListProblemDataTreeResult] =
      ProblemCommands
        .listProblemDataTree(databaseSession, actor, input)

  final class DownloadProblemDataPlan(problemDataStorage: ProblemDataStorage)
      extends PlainAuthenticatedHttpPlan[(ProblemSlug, ProblemDataFilename), DownloadProblemDataOutput]:

    override val name: String = "DownloadProblemData"

    override def execute(
      databaseSession: DatabaseSession,
      actor: AuthUser,
      input: (ProblemSlug, ProblemDataFilename)
    ): IO[DownloadProblemDataOutput] =
      val (problemSlug, filename) = input
      ProblemCommands
        .authorizeProblemDataDownload(problemDataStorage, databaseSession, actor, problemSlug)
        .map(authorization => DownloadProblemDataOutput(problemSlug, filename, authorization))

  final class DeleteProblemDataPlan(problemDataStorage: ProblemDataStorage)
      extends TransactionAuthenticatedHttpPlan[(ProblemSlug, ProblemDataFilename), ProblemCommands.DeleteProblemDataResult]:

    override val name: String = "DeleteProblemData"

    override def execute(
      connection: Connection,
      actor: AuthUser,
      input: (ProblemSlug, ProblemDataFilename)
    ): IO[ProblemCommands.DeleteProblemDataResult] =
      val (problemSlug, filename) = input
      ProblemCommands
        .deleteProblemData(problemDataStorage, connection, actor, problemSlug, filename)

  final class DeleteProblemDataPathPlan(problemDataStorage: ProblemDataStorage)
      extends TransactionAuthenticatedHttpPlan[(ProblemSlug, DeleteProblemDataPathRequest), ProblemCommands.DeleteProblemDataResult]:

    override val name: String = "DeleteProblemDataPath"

    override def execute(
      connection: Connection,
      actor: AuthUser,
      input: (ProblemSlug, DeleteProblemDataPathRequest)
    ): IO[ProblemCommands.DeleteProblemDataResult] =
      val (problemSlug, request) = input
      ProblemCommands
        .deleteProblemDataPath(problemDataStorage, connection, actor, problemSlug, request.path)

  final class ClearProblemDataPlan(problemDataStorage: ProblemDataStorage)
      extends TransactionAuthenticatedHttpPlan[ProblemSlug, ProblemCommands.ClearProblemDataResult]:

    override val name: String = "ClearProblemData"

    override def execute(
      connection: Connection,
      actor: AuthUser,
      input: ProblemSlug
    ): IO[ProblemCommands.ClearProblemDataResult] =
      ProblemCommands
        .clearProblemData(problemDataStorage, connection, actor, input)

  final class SetProblemReadyPlan(problemDataStorage: ProblemDataStorage)
      extends TransactionAuthenticatedHttpPlan[(ProblemSlug, SetProblemReadyRequest), ProblemCommands.SetProblemReadyResult]:

    override val name: String = "SetProblemReady"

    override def execute(
      connection: Connection,
      actor: AuthUser,
      input: (ProblemSlug, SetProblemReadyRequest)
    ): IO[ProblemCommands.SetProblemReadyResult] =
      val (problemSlug, request) = input
      ProblemCommands
        .setProblemDataReady(problemDataStorage, connection, actor, problemSlug, request.ready)

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

  case object DeleteProblem extends TransactionAuthenticatedHttpPlan[ProblemSlug, ProblemCommands.DeleteProblemResult]:

    override val name: String = "DeleteProblem"

    override def execute(
      connection: Connection,
      actor: AuthUser,
      input: ProblemSlug
    ): IO[ProblemCommands.DeleteProblemResult] =
      ProblemCommands
        .deleteProblem(connection, actor, input)
