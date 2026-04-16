package domains.problem.http

import cats.effect.IO
import database.DatabaseSession
import domains.auth.model.AuthUser
import domains.problem.application.ProblemCommands
import domains.problem.model.{CreateProblemRequest, ProblemDataFilename, ProblemSlug, UpdateProblemDataRequest, UpdateProblemRequest}
import domains.shared.model.PageRequest
import io.circe.syntax.*

import java.sql.Connection

object ProblemHttpPlans:

  final case class DownloadProblemDataOutput(
    problemSlug: ProblemSlug,
    filename: ProblemDataFilename,
    authorization: ProblemCommands.AuthorizeProblemDataDownloadResult
  )

  case object ListProblems extends PlainProblemHttpPlan[Unit, domains.shared.model.PageResponse[domains.problem.model.ProblemSummary]]:

    override val name: String = "ListProblems"

    override def execute(
      databaseSession: DatabaseSession,
      actor: AuthUser,
      input: Unit
    ): IO[domains.shared.model.PageResponse[domains.problem.model.ProblemSummary]] =
      ProblemCommands
        .listProblems(databaseSession, actor, PageRequest())

  case object CreateProblem extends TransactionProblemHttpPlan[CreateProblemRequest, ProblemCommands.CreateProblemResult]:

    override val name: String = "CreateProblem"

    override def execute(
      connection: Connection,
      actor: AuthUser,
      input: CreateProblemRequest
    ): IO[ProblemCommands.CreateProblemResult] =
      ProblemCommands
        .createProblem(connection, actor, input)

  case object GetProblem extends PlainProblemHttpPlan[ProblemSlug, ProblemCommands.GetProblemResult]:

    override val name: String = "GetProblem"

    override def execute(
      databaseSession: DatabaseSession,
      actor: AuthUser,
      input: ProblemSlug
    ): IO[ProblemCommands.GetProblemResult] =
      ProblemCommands
        .getProblemBySlug(databaseSession, actor, input)

  case object ListProblemData extends PlainProblemHttpPlan[ProblemSlug, ProblemCommands.ListProblemDataResult]:

    override val name: String = "ListProblemData"

    override def execute(
      databaseSession: DatabaseSession,
      actor: AuthUser,
      input: ProblemSlug
    ): IO[ProblemCommands.ListProblemDataResult] =
      ProblemCommands
        .listProblemData(databaseSession, actor, input)

  case object DownloadProblemData extends PlainProblemHttpPlan[(ProblemSlug, ProblemDataFilename), DownloadProblemDataOutput]:

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

  case object DeleteProblemData extends TransactionProblemHttpPlan[(ProblemSlug, ProblemDataFilename), ProblemCommands.DeleteProblemDataResult]:

    override val name: String = "DeleteProblemData"

    override def execute(
      connection: Connection,
      actor: AuthUser,
      input: (ProblemSlug, ProblemDataFilename)
    ): IO[ProblemCommands.DeleteProblemDataResult] =
      val (problemSlug, filename) = input
      ProblemCommands
        .deleteProblemData(connection, actor, problemSlug, filename)

  case object ClearProblemData extends TransactionProblemHttpPlan[ProblemSlug, ProblemCommands.ClearProblemDataResult]:

    override val name: String = "ClearProblemData"

    override def execute(
      connection: Connection,
      actor: AuthUser,
      input: ProblemSlug
    ): IO[ProblemCommands.ClearProblemDataResult] =
      ProblemCommands
        .clearProblemData(connection, actor, input)

  case object UpdateProblem extends TransactionProblemHttpPlan[(ProblemSlug, UpdateProblemRequest), ProblemCommands.UpdateProblemResult]:

    override val name: String = "UpdateProblem"

    override def execute(
      connection: Connection,
      actor: AuthUser,
      input: (ProblemSlug, UpdateProblemRequest)
    ): IO[ProblemCommands.UpdateProblemResult] =
      val (problemSlug, request) = input
      ProblemCommands
        .updateProblem(connection, actor, problemSlug, request)

  case object UpdateProblemData extends TransactionProblemHttpPlan[(ProblemSlug, UpdateProblemDataRequest), ProblemCommands.UpdateProblemDataResult]:

    override val name: String = "UpdateProblemData"

    override def execute(
      connection: Connection,
      actor: AuthUser,
      input: (ProblemSlug, UpdateProblemDataRequest)
    ): IO[ProblemCommands.UpdateProblemDataResult] =
      val (problemSlug, request) = input
      ProblemCommands
        .updateProblemData(connection, actor, problemSlug, request)

  case object DeleteProblem extends TransactionProblemHttpPlan[ProblemSlug, ProblemCommands.DeleteProblemResult]:

    override val name: String = "DeleteProblem"

    override def execute(
      connection: Connection,
      actor: AuthUser,
      input: ProblemSlug
    ): IO[ProblemCommands.DeleteProblemResult] =
      ProblemCommands
        .deleteProblem(connection, actor, input)
