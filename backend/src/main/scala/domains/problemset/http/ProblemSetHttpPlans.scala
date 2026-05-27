package domains.problemset.http



import cats.effect.IO
import database.DatabaseSession
import domains.auth.objects.AuthUser
import domains.problem.objects.ProblemSlug
import domains.problemset.application.ProblemSetCommands
import shared.http.{PlainAuthenticatedHttpPlan, TransactionAuthenticatedHttpPlan}
import domains.problemset.objects.request.{AddProblemToProblemSetRequest, CreateProblemSetRequest, UpdateProblemSetRequest}
import domains.problemset.objects.{ProblemSetSlug}
import shared.objects.{PageRequest, PageResponse}

import java.sql.Connection

object ProblemSetHttpPlans:

  case object ListProblemSets extends PlainAuthenticatedHttpPlan[AuthUser, PageRequest, PageResponse[domains.problemset.objects.response.ProblemSetSummary]]:

    override def execute(
      databaseSession: DatabaseSession,
      actor: AuthUser,
      input: PageRequest
    ): IO[PageResponse[domains.problemset.objects.response.ProblemSetSummary]] =
      ProblemSetCommands.listProblemSets(databaseSession, actor, input)

  case object GetProblemSet extends PlainAuthenticatedHttpPlan[AuthUser, ProblemSetSlug, ProblemSetCommands.GetProblemSetResult]:

    override def execute(
      databaseSession: DatabaseSession,
      actor: AuthUser,
      input: ProblemSetSlug
    ): IO[ProblemSetCommands.GetProblemSetResult] =
      ProblemSetCommands.getProblemSetBySlug(databaseSession, actor, input)

  case object CreateProblemSet extends TransactionAuthenticatedHttpPlan[AuthUser, CreateProblemSetRequest, ProblemSetCommands.CreateProblemSetResult]:

    override def execute(
      connection: Connection,
      actor: AuthUser,
      input: CreateProblemSetRequest
    ): IO[ProblemSetCommands.CreateProblemSetResult] =
      ProblemSetCommands.createProblemSet(connection, actor, input)

  case object AddProblem extends TransactionAuthenticatedHttpPlan[AuthUser, (ProblemSetSlug, AddProblemToProblemSetRequest), ProblemSetCommands.AddProblemResult]:

    override def execute(
      connection: Connection,
      actor: AuthUser,
      input: (ProblemSetSlug, AddProblemToProblemSetRequest)
    ): IO[ProblemSetCommands.AddProblemResult] =
      val (problemSetSlug, request) = input
      ProblemSetCommands.addProblemToProblemSet(connection, actor, problemSetSlug, request)

  case object UpdateProblemSet extends TransactionAuthenticatedHttpPlan[AuthUser, (ProblemSetSlug, UpdateProblemSetRequest), ProblemSetCommands.UpdateProblemSetResult]:

    override def execute(
      connection: Connection,
      actor: AuthUser,
      input: (ProblemSetSlug, UpdateProblemSetRequest)
    ): IO[ProblemSetCommands.UpdateProblemSetResult] =
      val (problemSetSlug, request) = input
      ProblemSetCommands.updateProblemSet(connection, actor, problemSetSlug, request)

  case object DeleteProblemSet extends TransactionAuthenticatedHttpPlan[AuthUser, ProblemSetSlug, ProblemSetCommands.DeleteProblemSetResult]:

    override def execute(
      connection: Connection,
      actor: AuthUser,
      input: ProblemSetSlug
    ): IO[ProblemSetCommands.DeleteProblemSetResult] =
      ProblemSetCommands.deleteProblemSet(connection, actor, input)

  case object RemoveProblem extends TransactionAuthenticatedHttpPlan[AuthUser, (ProblemSetSlug, ProblemSlug), ProblemSetCommands.RemoveProblemResult]:

    override def execute(
      connection: Connection,
      actor: AuthUser,
      input: (ProblemSetSlug, ProblemSlug)
    ): IO[ProblemSetCommands.RemoveProblemResult] =
      val (problemSetSlug, problemSlug) = input
      ProblemSetCommands.removeProblemFromProblemSet(connection, actor, problemSetSlug, problemSlug)
