package domains.problemset.http



import cats.effect.IO
import database.DatabaseSession
import domains.auth.model.AuthUser
import domains.problem.model.ProblemSlug
import domains.problemset.application.ProblemSetCommands
import domains.shared.http.{PlainAuthenticatedHttpPlan, TransactionAuthenticatedHttpPlan}
import domains.problemset.http.request.{AddProblemToProblemSetRequest, CreateProblemSetRequest, UpdateProblemSetRequest}
import domains.problemset.model.{ProblemSetSlug}
import domains.shared.model.{PageRequest, PageResponse}

import java.sql.Connection

object ProblemSetHttpPlans:

  case object ListProblemSets extends PlainAuthenticatedHttpPlan[PageRequest, PageResponse[domains.problemset.application.view.ProblemSetSummary]]:

    override val name: String = "ListProblemSets"

    override def execute(
      databaseSession: DatabaseSession,
      actor: AuthUser,
      input: PageRequest
    ): IO[PageResponse[domains.problemset.application.view.ProblemSetSummary]] =
      ProblemSetCommands.listProblemSets(databaseSession, actor, input)

  case object GetProblemSet extends PlainAuthenticatedHttpPlan[ProblemSetSlug, ProblemSetCommands.GetProblemSetResult]:

    override val name: String = "GetProblemSet"

    override def execute(
      databaseSession: DatabaseSession,
      actor: AuthUser,
      input: ProblemSetSlug
    ): IO[ProblemSetCommands.GetProblemSetResult] =
      ProblemSetCommands.getProblemSetBySlug(databaseSession, actor, input)

  case object CreateProblemSet extends TransactionAuthenticatedHttpPlan[CreateProblemSetRequest, ProblemSetCommands.CreateProblemSetResult]:

    override val name: String = "CreateProblemSet"

    override def execute(
      connection: Connection,
      actor: AuthUser,
      input: CreateProblemSetRequest
    ): IO[ProblemSetCommands.CreateProblemSetResult] =
      ProblemSetCommands.createProblemSet(connection, actor, input)

  case object AddProblem extends TransactionAuthenticatedHttpPlan[(ProblemSetSlug, AddProblemToProblemSetRequest), ProblemSetCommands.AddProblemResult]:

    override val name: String = "AddProblem"

    override def execute(
      connection: Connection,
      actor: AuthUser,
      input: (ProblemSetSlug, AddProblemToProblemSetRequest)
    ): IO[ProblemSetCommands.AddProblemResult] =
      val (problemSetSlug, request) = input
      ProblemSetCommands.addProblemToProblemSet(connection, actor, problemSetSlug, request)

  case object UpdateProblemSet extends TransactionAuthenticatedHttpPlan[(ProblemSetSlug, UpdateProblemSetRequest), ProblemSetCommands.UpdateProblemSetResult]:

    override val name: String = "UpdateProblemSet"

    override def execute(
      connection: Connection,
      actor: AuthUser,
      input: (ProblemSetSlug, UpdateProblemSetRequest)
    ): IO[ProblemSetCommands.UpdateProblemSetResult] =
      val (problemSetSlug, request) = input
      ProblemSetCommands.updateProblemSet(connection, actor, problemSetSlug, request)

  case object DeleteProblemSet extends TransactionAuthenticatedHttpPlan[ProblemSetSlug, ProblemSetCommands.DeleteProblemSetResult]:

    override val name: String = "DeleteProblemSet"

    override def execute(
      connection: Connection,
      actor: AuthUser,
      input: ProblemSetSlug
    ): IO[ProblemSetCommands.DeleteProblemSetResult] =
      ProblemSetCommands.deleteProblemSet(connection, actor, input)

  case object RemoveProblem extends TransactionAuthenticatedHttpPlan[(ProblemSetSlug, ProblemSlug), ProblemSetCommands.RemoveProblemResult]:

    override val name: String = "RemoveProblem"

    override def execute(
      connection: Connection,
      actor: AuthUser,
      input: (ProblemSetSlug, ProblemSlug)
    ): IO[ProblemSetCommands.RemoveProblemResult] =
      val (problemSetSlug, problemSlug) = input
      ProblemSetCommands.removeProblemFromProblemSet(connection, actor, problemSetSlug, problemSlug)
