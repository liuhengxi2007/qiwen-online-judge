package domains.problem.http



import cats.effect.IO
import database.DatabaseSession
import domains.auth.objects.AuthUser
import domains.problem.application.{ProblemCommands, ProblemDataStorage}
import domains.problem.objects.request.{CreateProblemRequest, DeleteProblemDataPathRequest, ProblemListRequest, ProblemSearchQuery, UpdateProblemRequest}
import domains.problem.objects.{ProblemDataFilename, ProblemSlug}
import domains.problem.objects.response.{ProblemSuggestion}
import shared.http.{PlainAuthenticatedHttpPlan, TransactionAuthenticatedHttpPlan}

import java.sql.Connection

object ProblemHttpPlans:

  final case class SetProblemReadyRequest(ready: Boolean)

  final case class DownloadProblemDataOutput(
    problemSlug: ProblemSlug,
    filename: ProblemDataFilename,
    authorization: ProblemCommands.AuthorizeProblemDataDownloadResult
  )

  case object ListProblems extends PlainAuthenticatedHttpPlan[AuthUser, ProblemListRequest, shared.objects.PageResponse[domains.problem.objects.response.ProblemSummary]]:

    override def execute(
      databaseSession: DatabaseSession,
      actor: AuthUser,
      input: ProblemListRequest
    ): IO[shared.objects.PageResponse[domains.problem.objects.response.ProblemSummary]] =
      ProblemCommands.listProblems(databaseSession, actor, input)

  case object CreateProblem extends TransactionAuthenticatedHttpPlan[AuthUser, CreateProblemRequest, ProblemCommands.CreateProblemResult]:

    override def execute(
      connection: Connection,
      actor: AuthUser,
      input: CreateProblemRequest
    ): IO[ProblemCommands.CreateProblemResult] =
      ProblemCommands
        .createProblem(connection, actor, input)

  case object ListProblemSuggestions extends PlainAuthenticatedHttpPlan[AuthUser, ProblemSearchQuery, List[ProblemSuggestion]]:

    override def execute(
      databaseSession: DatabaseSession,
      actor: AuthUser,
      input: ProblemSearchQuery
    ): IO[List[ProblemSuggestion]] =
      ProblemCommands.listProblemSuggestions(databaseSession, actor, input)

  case object GetProblem extends PlainAuthenticatedHttpPlan[AuthUser, ProblemSlug, ProblemCommands.GetProblemResult]:

    override def execute(
      databaseSession: DatabaseSession,
      actor: AuthUser,
      input: ProblemSlug
    ): IO[ProblemCommands.GetProblemResult] =
      ProblemCommands
        .getProblemBySlug(databaseSession, actor, input)

  final class ListProblemDataPlan(problemDataStorage: ProblemDataStorage)
      extends PlainAuthenticatedHttpPlan[AuthUser, ProblemSlug, ProblemCommands.ListProblemDataResult]:

    override def execute(
      databaseSession: DatabaseSession,
      actor: AuthUser,
      input: ProblemSlug
    ): IO[ProblemCommands.ListProblemDataResult] =
      ProblemCommands
        .listProblemData(problemDataStorage, databaseSession, actor, input)

  case object ListProblemDataTree extends PlainAuthenticatedHttpPlan[AuthUser, ProblemSlug, ProblemCommands.ListProblemDataTreeResult]:

    override def execute(
      databaseSession: DatabaseSession,
      actor: AuthUser,
      input: ProblemSlug
    ): IO[ProblemCommands.ListProblemDataTreeResult] =
      ProblemCommands
        .listProblemDataTree(databaseSession, actor, input)

  final class DownloadProblemDataPlan(problemDataStorage: ProblemDataStorage)
      extends PlainAuthenticatedHttpPlan[AuthUser, (ProblemSlug, ProblemDataFilename), DownloadProblemDataOutput]:

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
      extends TransactionAuthenticatedHttpPlan[AuthUser, (ProblemSlug, ProblemDataFilename), ProblemCommands.DeleteProblemDataResult]:

    override def execute(
      connection: Connection,
      actor: AuthUser,
      input: (ProblemSlug, ProblemDataFilename)
    ): IO[ProblemCommands.DeleteProblemDataResult] =
      val (problemSlug, filename) = input
      ProblemCommands
        .deleteProblemData(problemDataStorage, connection, actor, problemSlug, filename)

  final class DeleteProblemDataPathPlan(problemDataStorage: ProblemDataStorage)
      extends TransactionAuthenticatedHttpPlan[AuthUser, (ProblemSlug, DeleteProblemDataPathRequest), ProblemCommands.DeleteProblemDataResult]:

    override def execute(
      connection: Connection,
      actor: AuthUser,
      input: (ProblemSlug, DeleteProblemDataPathRequest)
    ): IO[ProblemCommands.DeleteProblemDataResult] =
      val (problemSlug, request) = input
      ProblemCommands
        .deleteProblemDataPath(problemDataStorage, connection, actor, problemSlug, request.path)

  final class ClearProblemDataPlan(problemDataStorage: ProblemDataStorage)
      extends TransactionAuthenticatedHttpPlan[AuthUser, ProblemSlug, ProblemCommands.ClearProblemDataResult]:

    override def execute(
      connection: Connection,
      actor: AuthUser,
      input: ProblemSlug
    ): IO[ProblemCommands.ClearProblemDataResult] =
      ProblemCommands
        .clearProblemData(problemDataStorage, connection, actor, input)

  final class SetProblemReadyPlan(problemDataStorage: ProblemDataStorage)
      extends TransactionAuthenticatedHttpPlan[AuthUser, (ProblemSlug, SetProblemReadyRequest), ProblemCommands.SetProblemReadyResult]:

    override def execute(
      connection: Connection,
      actor: AuthUser,
      input: (ProblemSlug, SetProblemReadyRequest)
    ): IO[ProblemCommands.SetProblemReadyResult] =
      val (problemSlug, request) = input
      ProblemCommands
        .setProblemDataReady(problemDataStorage, connection, actor, problemSlug, request.ready)

  case object UpdateProblem extends TransactionAuthenticatedHttpPlan[AuthUser, (ProblemSlug, UpdateProblemRequest), ProblemCommands.UpdateProblemResult]:

    override def execute(
      connection: Connection,
      actor: AuthUser,
      input: (ProblemSlug, UpdateProblemRequest)
    ): IO[ProblemCommands.UpdateProblemResult] =
      val (problemSlug, request) = input
      ProblemCommands
        .updateProblem(connection, actor, problemSlug, request)

  case object DeleteProblem extends TransactionAuthenticatedHttpPlan[AuthUser, ProblemSlug, ProblemCommands.DeleteProblemResult]:

    override def execute(
      connection: Connection,
      actor: AuthUser,
      input: ProblemSlug
    ): IO[ProblemCommands.DeleteProblemResult] =
      ProblemCommands
        .deleteProblem(connection, actor, input)
