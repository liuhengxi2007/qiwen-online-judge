package domains.submission.api

import cats.effect.IO
import cats.syntax.all.*
import domains.auth.api.AuthenticatedApi
import domains.auth.objects.internal.AuthenticatedUser
import domains.contest.objects.ContestId
import domains.judge.utils.JudgeTaskBuilder
import domains.problem.api.{EvaluateProblemAccess, GetProblemSubmissionResultDisplayMode}
import domains.problem.objects.{ProblemDataPath, ProblemId, ProblemSlug, ProblemTitle}
import domains.problem.table.problem_data_file.ProblemDataFileTable
import domains.problem.utils.ProblemDataStorage

import domains.submission.objects.{SubmissionSource, SubmissionSourceCode}
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

final case class CreateSubmission(
  submissionProgramStorage: SubmissionProgramStorage,
  problemDataStorage: ProblemDataStorage
) extends AuthenticatedApi[CreateSubmissionRequest, SubmissionDetail]:

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
      programs <- validatePrograms(request.programs)
      validRequest = request.copy(problemSlug = problemSlug, programs = programs)
      access <- EvaluateProblemAccess.plan(connection, actor, validRequest.problemSlug)
      problem <- access.problem match
        case Some(problem) => IO.pure(problem)
        case None => HttpApiError.raise(HttpApiError.notFound(ApiMessages.problemNotFound))
      _ <- HttpApiError.ensure(access.canView, HttpApiError.notFound(ApiMessages.problemNotFound))
      created <- createForAccessibleProblem(
        connection,
        actor,
        validRequest,
        problem.id,
        contestId = None,
        problem.slug,
        problem.title,
        source = SubmissionSource.FromProblemSet,
        access.canManage
      )
    yield created

  def createForAccessibleProblem(
    connection: Connection,
    actor: AuthenticatedUser,
    request: CreateSubmissionRequest,
    problemId: ProblemId,
    contestId: Option[ContestId],
    problemSlug: ProblemSlug,
    problemTitle: ProblemTitle,
    source: SubmissionSource,
    canManage: Boolean
  ): IO[SubmissionDetail] =
    for
      programs <- validatePrograms(request.programs)
      validRequest = request.copy(problemSlug = problemSlug, programs = programs)
      submissionUuid <- IO.randomUUID
      resultDisplayMode <- GetProblemSubmissionResultDisplayMode.plan(connection, problemId).flatMap {
        case Some(value) => IO.pure(value)
        case None => HttpApiError.raise(HttpApiError.notFound(ApiMessages.problemNotFound))
      }
      _ <- validateReadyJudgeConfig(connection, problemId, problemSlug, validRequest.programs)
      manifestInput = validRequest.programs.map { case (role, program) => role -> (program.language -> program.sourceCode) }
      programManifest <- HttpApiError.fromEitherBadRequest(SubmissionProgramManifest.fromPrograms(submissionUuid, manifestInput))
      _ <- programManifest.programs.toList.traverse_ { case (role, program) =>
        submissionProgramStorage.writeSource(program.sourceKey, validRequest.programs(role).sourceCode)
      }
      defaultSourceCode = validRequest.programs(programManifest.defaultProgramKey).sourceCode
      created <- SubmissionMutationTable
        .insert(
          connection = connection,
          submissionUuid = submissionUuid,
          problemId = problemId,
          contestId = contestId,
          problemSlug = problemSlug,
          problemTitle = problemTitle,
          resultDisplayMode = resultDisplayMode,
          source = source,
          submitterUsername = actor.username,
          programManifest = programManifest,
          sourceCode = defaultSourceCode
        )
        .handleErrorWith { error =>
          submissionProgramStorage.deleteManifest(programManifest).handleError(_ => ()).void *> IO.raiseError(error)
        }
      responsePrograms = programManifest.programs.map { case (role, program) =>
        role -> SubmissionDetail.Program(program.language, validRequest.programs(role).sourceCode)
      }
    yield created.copy(canManage = canManage, programs = responsePrograms)

  private def validatePrograms(
    programs: Map[String, CreateSubmissionRequest.Program]
  ): IO[Map[String, CreateSubmissionRequest.Program]] =
    val normalized = programs.toList.map { case (role, program) => role.trim -> program }
    val parsed = normalized.traverse { case (role, program) =>
      SubmissionSourceCode.parse(program.sourceCode.value).map(sourceCode => role -> program.copy(sourceCode = sourceCode))
    }.map(_.toMap)
    HttpApiError.fromEitherBadRequest(parsed.flatMap { validPrograms =>
      val manifestInput = validPrograms.map { case (role, program) => role -> (program.language -> program.sourceCode) }
      SubmissionProgramManifest
        .fromPrograms(java.util.UUID.fromString("00000000-0000-4000-8000-000000000000"), manifestInput)
        .map(_ => validPrograms)
    })

  private def validateReadyJudgeConfig(
    connection: Connection,
    problemId: ProblemId,
    problemSlug: ProblemSlug,
    programs: Map[String, CreateSubmissionRequest.Program]
  ): IO[Unit] =
    val judgeYamlPath = ProblemDataPath("judge.yaml")
    for
      manifest <- ProblemDataFileTable.manifestForProblem(connection, problemId, problemSlug)
      maybeConfig <- problemDataStorage.readPath(problemSlug, judgeYamlPath)
      configBytes <- maybeConfig match
        case Some((_, bytes)) => IO.pure(bytes)
        case None => HttpApiError.raise(HttpApiError.badRequest("judge.yaml is required at the problem data root."))
      programLanguages = programs.map { case (role, program) => role -> program.language }
      _ <- HttpApiError.fromEitherBadRequest(JudgeTaskBuilder.validateSubmissionProgramsForConfig(configBytes, programLanguages, manifest))
    yield ()
