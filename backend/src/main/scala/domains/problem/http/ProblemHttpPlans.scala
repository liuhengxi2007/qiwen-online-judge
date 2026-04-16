package domains.problem.http

import cats.effect.IO
import database.DatabaseSession
import domains.auth.model.AuthUser
import domains.problem.application.ProblemCommands
import domains.problem.model.{CreateProblemRequest, ProblemDataFilename, ProblemSlug, UpdateProblemDataRequest, UpdateProblemRequest}
import domains.shared.model.PageRequest
import io.circe.syntax.*
import org.http4s.{Response, Status}
import org.http4s.circe.CirceEntityEncoder.*

object ProblemHttpPlans:

  case object ListProblems extends ProblemHttpPlan[Unit]:

    override val name: String = "ListProblems"

    override def execute(
      databaseSession: DatabaseSession,
      actor: AuthUser,
      input: Unit
    ): IO[Response[IO]] =
      ProblemCommands
        .listProblems(databaseSession, actor, PageRequest())
        .map(response => Response[IO](status = Status.Ok).withEntity(response.asJson))

  case object CreateProblem extends ProblemHttpPlan[CreateProblemRequest]:

    override val name: String = "CreateProblem"

    override def execute(
      databaseSession: DatabaseSession,
      actor: AuthUser,
      input: CreateProblemRequest
    ): IO[Response[IO]] =
      ProblemCommands
        .createProblem(databaseSession, actor, input)
        .flatMap(ProblemHttpResponses.mapCreateResult)

  case object GetProblem extends ProblemHttpPlan[ProblemSlug]:

    override val name: String = "GetProblem"

    override def execute(
      databaseSession: DatabaseSession,
      actor: AuthUser,
      input: ProblemSlug
    ): IO[Response[IO]] =
      ProblemCommands
        .getProblemBySlug(databaseSession, actor, input)
        .flatMap(ProblemHttpResponses.mapGetResult)

  case object ListProblemData extends ProblemHttpPlan[ProblemSlug]:

    override val name: String = "ListProblemData"

    override def execute(
      databaseSession: DatabaseSession,
      actor: AuthUser,
      input: ProblemSlug
    ): IO[Response[IO]] =
      ProblemCommands
        .listProblemData(databaseSession, actor, input)
        .flatMap(ProblemHttpResponses.mapListDataResult)

  case object DownloadProblemData extends ProblemHttpPlan[(ProblemSlug, ProblemDataFilename)]:

    override val name: String = "DownloadProblemData"

    override def execute(
      databaseSession: DatabaseSession,
      actor: AuthUser,
      input: (ProblemSlug, ProblemDataFilename)
    ): IO[Response[IO]] =
      val (problemSlug, filename) = input
      ProblemCommands
        .authorizeProblemDataDownload(databaseSession, actor, problemSlug)
        .flatMap {
          case ProblemCommands.AuthorizeProblemDataDownloadResult.Authorized =>
            ProblemHttpResponses.downloadDataResponse(problemSlug, filename)
          case other =>
            ProblemHttpResponses.mapAuthorizeDownloadResult(other)
        }

  case object DeleteProblemData extends ProblemHttpPlan[(ProblemSlug, ProblemDataFilename)]:

    override val name: String = "DeleteProblemData"

    override def execute(
      databaseSession: DatabaseSession,
      actor: AuthUser,
      input: (ProblemSlug, ProblemDataFilename)
    ): IO[Response[IO]] =
      val (problemSlug, filename) = input
      ProblemCommands
        .deleteProblemData(databaseSession, actor, problemSlug, filename)
        .flatMap(ProblemHttpResponses.mapDeleteDataResult)

  case object ClearProblemData extends ProblemHttpPlan[ProblemSlug]:

    override val name: String = "ClearProblemData"

    override def execute(
      databaseSession: DatabaseSession,
      actor: AuthUser,
      input: ProblemSlug
    ): IO[Response[IO]] =
      ProblemCommands
        .clearProblemData(databaseSession, actor, input)
        .flatMap(ProblemHttpResponses.mapClearDataResult)

  case object UpdateProblem extends ProblemHttpPlan[(ProblemSlug, UpdateProblemRequest)]:

    override val name: String = "UpdateProblem"

    override def execute(
      databaseSession: DatabaseSession,
      actor: AuthUser,
      input: (ProblemSlug, UpdateProblemRequest)
    ): IO[Response[IO]] =
      val (problemSlug, request) = input
      ProblemCommands
        .updateProblem(databaseSession, actor, problemSlug, request)
        .flatMap(ProblemHttpResponses.mapUpdateResult)

  case object UpdateProblemData extends ProblemHttpPlan[(ProblemSlug, UpdateProblemDataRequest)]:

    override val name: String = "UpdateProblemData"

    override def execute(
      databaseSession: DatabaseSession,
      actor: AuthUser,
      input: (ProblemSlug, UpdateProblemDataRequest)
    ): IO[Response[IO]] =
      val (problemSlug, request) = input
      ProblemCommands
        .updateProblemData(databaseSession, actor, problemSlug, request)
        .flatMap(ProblemHttpResponses.mapUpdateDataResult)

  case object DeleteProblem extends ProblemHttpPlan[ProblemSlug]:

    override val name: String = "DeleteProblem"

    override def execute(
      databaseSession: DatabaseSession,
      actor: AuthUser,
      input: ProblemSlug
    ): IO[Response[IO]] =
      ProblemCommands
        .deleteProblem(databaseSession, actor, input)
        .flatMap(ProblemHttpResponses.mapDeleteResult)
