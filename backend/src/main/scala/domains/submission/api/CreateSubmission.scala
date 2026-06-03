package domains.submission.api

import cats.effect.IO
import domains.auth.api.AuthenticatedApi
import domains.auth.objects.internal.AuthenticatedUser
import domains.problem.api.EvaluateProblemAccess
import domains.problem.objects.{ProblemId, ProblemSlug, ProblemTitle}

import domains.submission.objects.SubmissionSourceCode
import domains.submission.objects.internal.SubmissionProgramManifest
import domains.submission.objects.request.CreateSubmissionRequest
import domains.submission.objects.response.SubmissionDetail
import domains.submission.table.submission.SubmissionMutationTable
import domains.submission.utils.SubmissionProgramStorage
import io.circe.Encoder
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.{Method, Request, Status}
import shared.api.{ApiMessages, ApiPath, HttpApiError, PathParams}

import java.sql.Connection

final case class CreateSubmission(submissionProgramStorage: SubmissionProgramStorage) extends AuthenticatedApi[CreateSubmissionRequest, SubmissionDetail]:

  override val method: Method = Method.POST
  override val path: ApiPath = ApiPath("/api/submissions")
  override val successStatus: Status = Status.Created
  override protected val outputEncoder: Encoder[SubmissionDetail] = summon[Encoder[SubmissionDetail]]

  override def decode(request: Request[IO], pathParams: PathParams): IO[CreateSubmissionRequest] =
    val _ = pathParams
    request.as[CreateSubmissionRequest]

  override def plan(connection: Connection, actor: AuthenticatedUser, request: CreateSubmissionRequest): IO[SubmissionDetail] =
    createForProblem(connection, actor, request)

  def createForProblem(connection: Connection, actor: AuthenticatedUser, request: CreateSubmissionRequest): IO[SubmissionDetail] =
    for
      problemSlug <- HttpApiError.fromEitherBadRequest(ProblemSlug.parse(request.problemSlug.value))
      sourceCode <- HttpApiError.fromEitherBadRequest(SubmissionSourceCode.parse(request.sourceCode.value))
      validRequest = request.copy(problemSlug = problemSlug, sourceCode = sourceCode)
      access <- EvaluateProblemAccess.plan(connection, actor, validRequest.problemSlug)
      problem <- access.problem match
        case Some(problem) => IO.pure(problem)
        case None => HttpApiError.raise(HttpApiError.notFound(ApiMessages.problemNotFound))
      _ <- HttpApiError.ensure(access.canView, HttpApiError.notFound(ApiMessages.problemNotFound))
      created <- createForAccessibleProblem(connection, actor, validRequest, problem.id, problem.slug, problem.title, access.canManage)
    yield created

  def createForAccessibleProblem(
    connection: Connection,
    actor: AuthenticatedUser,
    request: CreateSubmissionRequest,
    problemId: ProblemId,
    problemSlug: ProblemSlug,
    problemTitle: ProblemTitle,
    canManage: Boolean
  ): IO[SubmissionDetail] =
    for
      sourceCode <- HttpApiError.fromEitherBadRequest(SubmissionSourceCode.parse(request.sourceCode.value))
      validRequest = request.copy(problemSlug = problemSlug, sourceCode = sourceCode)
      submissionUuid <- IO.randomUUID
      programManifest = SubmissionProgramManifest.singleDefault(submissionUuid, validRequest.language, validRequest.sourceCode)
      _ <- submissionProgramStorage.writeSource(
        programManifest.programs(SubmissionProgramManifest.DefaultProgramKey).sourceKey,
        validRequest.sourceCode
      )
      created <- SubmissionMutationTable
        .insert(
          connection = connection,
          submissionUuid = submissionUuid,
          problemId = problemId,
          problemSlug = problemSlug,
          problemTitle = problemTitle,
          submitterUsername = actor.username,
          programManifest = programManifest,
          sourceCode = validRequest.sourceCode
        )
        .handleErrorWith { error =>
          submissionProgramStorage.deleteManifest(programManifest).handleError(_ => ()).void *> IO.raiseError(error)
        }
    yield created.copy(canManage = canManage)
