package domains.problem.http

import cats.effect.IO
import database.DatabaseSession
import domains.auth.application.SessionStore
import domains.auth.http.AuthHttpSessionSupport
import domains.problem.model.{CreateProblemRequest, ProblemDataFilename, ProblemSlug, UpdateProblemDataRequest, UpdateProblemRequest}
import org.http4s.{Request, Response}
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.dsl.Http4sDsl

final class ProblemHttpHandlers(
  databaseSession: DatabaseSession,
  sessionStore: SessionStore
)(using dsl: Http4sDsl[IO]):

  private def runAuthenticatedPlan[Input](
    request: Request[IO],
    input: Input,
    plan: ProblemHttpPlan[Input]
  ): IO[Response[IO]] =
    AuthHttpSessionSupport.withAuthenticatedUser(databaseSession, sessionStore, request) { actor =>
      plan.execute(databaseSession, actor, input)
    }

  private def runDecodedAuthenticatedPlan[Body, Input](
    request: Request[IO],
    plan: ProblemHttpPlan[Input]
  )(
    toInput: Body => Input
  )(using org.http4s.EntityDecoder[IO, Body]): IO[Response[IO]] =
    AuthHttpSessionSupport.withAuthenticatedUser(databaseSession, sessionStore, request) { actor =>
      request.as[Body].flatMap(body => plan.execute(databaseSession, actor, toInput(body)))
    }

  def listProblems(request: Request[IO]): IO[Response[IO]] =
    runAuthenticatedPlan(request, (), ProblemHttpPlans.ListProblems)

  def createProblem(request: Request[IO]): IO[Response[IO]] =
    runDecodedAuthenticatedPlan[CreateProblemRequest, CreateProblemRequest](request, ProblemHttpPlans.CreateProblem)(identity)

  def getProblem(request: Request[IO], parsedProblemSlug: ProblemSlug): IO[Response[IO]] =
    runAuthenticatedPlan(request, parsedProblemSlug, ProblemHttpPlans.GetProblem)

  def listProblemData(request: Request[IO], parsedProblemSlug: ProblemSlug): IO[Response[IO]] =
    runAuthenticatedPlan(request, parsedProblemSlug, ProblemHttpPlans.ListProblemData)

  def downloadProblemData(
    request: Request[IO],
    parsedProblemSlug: ProblemSlug,
    parsedFilename: ProblemDataFilename
  ): IO[Response[IO]] =
    runAuthenticatedPlan(request, (parsedProblemSlug, parsedFilename), ProblemHttpPlans.DownloadProblemData)

  def deleteProblemData(
    request: Request[IO],
    parsedProblemSlug: ProblemSlug,
    parsedFilename: ProblemDataFilename
  ): IO[Response[IO]] =
    runAuthenticatedPlan(request, (parsedProblemSlug, parsedFilename), ProblemHttpPlans.DeleteProblemData)

  def clearProblemData(request: Request[IO], parsedProblemSlug: ProblemSlug): IO[Response[IO]] =
    runAuthenticatedPlan(request, parsedProblemSlug, ProblemHttpPlans.ClearProblemData)

  def updateProblem(request: Request[IO], parsedProblemSlug: ProblemSlug): IO[Response[IO]] =
    runDecodedAuthenticatedPlan[UpdateProblemRequest, (ProblemSlug, UpdateProblemRequest)](request, ProblemHttpPlans.UpdateProblem) {
      updateRequest => (parsedProblemSlug, updateRequest)
    }

  def updateProblemData(request: Request[IO], parsedProblemSlug: ProblemSlug): IO[Response[IO]] =
    runDecodedAuthenticatedPlan[UpdateProblemDataRequest, (ProblemSlug, UpdateProblemDataRequest)](request, ProblemHttpPlans.UpdateProblemData) {
      updateDataRequest => (parsedProblemSlug, updateDataRequest)
    }

  def deleteProblem(request: Request[IO], parsedProblemSlug: ProblemSlug): IO[Response[IO]] =
    runAuthenticatedPlan(request, parsedProblemSlug, ProblemHttpPlans.DeleteProblem)
