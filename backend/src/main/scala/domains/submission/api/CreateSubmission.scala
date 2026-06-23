package domains.submission.api

import cats.effect.IO
import cats.syntax.all.*
import domains.auth.api.AuthenticatedApi
import domains.auth.objects.internal.AuthenticatedUser
import domains.contest.objects.ContestId
import domains.judge.utils.JudgeTaskBuilder
import domains.problem.api.{EvaluateProblemAccess, GetJudgeProblemDataManifest, GetProblemSubmissionResultDisplayMode}
import domains.problem.objects.{ProblemDataPath, ProblemId, ProblemSlug, ProblemTitle}
import domains.problem.utils.{ProblemDataStorage, ProblemDataStorageContext}

import domains.submission.objects.{SubmissionSource, SubmissionSourceCode}
import domains.submission.objects.internal.SubmissionProgramManifest
import domains.submission.objects.request.{CreateSubmissionMultipartProgram, CreateSubmissionRequest}
import domains.submission.objects.response.SubmissionDetail
import domains.submission.table.submission.SubmissionMutationTable
import domains.submission.utils.{SubmissionProgramStorage, SubmissionProgramStorageContext}
import io.circe.Encoder
import io.circe.parser.decode as decodeJson
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.multipart.Multipart
import org.http4s.{Method, Request, Status}
import shared.api.{ApiMessages, ApiPath, HttpApiError, MultipartTextSupport, PathParams}

import java.sql.Connection

/** 创建普通题目提交的 API；校验题目可见、判题配置和程序角色后写入提交与源码对象。 */
final case class CreateSubmission(
  submissionProgramStorage: SubmissionProgramStorageContext,
  problemDataStorage: ProblemDataStorageContext
) extends AuthenticatedApi[CreateSubmissionRequest, SubmissionDetail]:

  override val method: Method = Method.POST
  override val path: ApiPath = ApiPath("/api/submissions")
  override val successStatus: Status = Status.Created
  override protected val outputEncoder: Encoder[SubmissionDetail] = summon[Encoder[SubmissionDetail]]

  /** 支持 JSON 或 multipart 请求解析提交源码；路径参数无业务含义。 */
  override def decode(request: Request[IO], pathParams: PathParams): IO[CreateSubmissionRequest] =
    val _ = pathParams
    CreateSubmission.decodeRequest(request)

  /** 为可见题目创建提交；不可见或不存在的题目统一返回题目不存在。 */
  override def plan(connection: Connection, actor: AuthenticatedUser, request: CreateSubmissionRequest): IO[SubmissionDetail] =
    createForProblem(connection, actor, request)

  /** 普通题目提交入口；校验题目访问权限后委托给通用创建流程。 */
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

  /** 为已确认可提交的题目创建提交；会写源码对象、插入提交行，插入失败时尽量删除已写源码。 */
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
        SubmissionProgramStorage.writeSource(submissionProgramStorage, program.sourceKey, validRequest.programs(role).sourceCode)
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
          SubmissionProgramStorage.deleteManifest(submissionProgramStorage, programManifest).handleError(_ => ()).void *> IO.raiseError(error)
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
        // 注意：固定 UUID 只用于提交请求的角色/语言/源码清单校验，真实源码对象 key 会在创建提交时重新生成。
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
      manifest <- GetJudgeProblemDataManifest
        .plan(connection, GetJudgeProblemDataManifest.input(problemId, problemSlug))
        .flatMap {
          case Some(manifest) => IO.pure(manifest)
          case None => HttpApiError.raise(HttpApiError.notFound(ApiMessages.problemNotFound))
        }
      maybeConfig <- ProblemDataStorage.readPath(problemDataStorage, problemSlug, judgeYamlPath)
      configBytes <- maybeConfig match
        case Some((_, bytes)) => IO.pure(bytes)
        case None => HttpApiError.raise(HttpApiError.badRequest("judge.yaml is required at the problem data root."))
      programLanguages = programs.map { case (role, program) => role -> program.language }
      _ <- HttpApiError.fromEitherBadRequest(JudgeTaskBuilder.validateSubmissionProgramsForConfig(configBytes, programLanguages, manifest))
    yield ()

/** 提交创建请求解析辅助；处理 JSON 与 multipart 两种输入形态。 */
object CreateSubmission:

  private val ScalarMaxBytes: Long = 64L * 1024L
  private val SourceMaxBytes: Long = SubmissionSourceCode.MaxChars.toLong * 4L

  /** 根据请求 Content-Type 解析提交请求；multipart 会按 programs 描述读取多个源码 part。 */
  def decodeRequest(request: Request[IO]): IO[CreateSubmissionRequest] =
    if MultipartTextSupport.isMultipart(request) then decodeMultipart(request)
    else request.as[CreateSubmissionRequest]

  private def decodeMultipart(request: Request[IO]): IO[CreateSubmissionRequest] =
    for
      multipart <- request.as[Multipart[IO]]
      problemSlugText <- MultipartTextSupport.requireUtf8Text(multipart, "problemSlug", ScalarMaxBytes)
      problemSlug <- HttpApiError.fromEitherBadRequest(domains.problem.objects.ProblemSlug.parse(problemSlugText))
      programsText <- MultipartTextSupport.requireUtf8Text(multipart, "programs", ScalarMaxBytes)
      programSpecs <- HttpApiError.fromEitherBadRequest(
        decodeJson[List[CreateSubmissionMultipartProgram]](programsText).left.map(error => s"Multipart programs field is invalid JSON: ${error.getMessage}")
      )
      _ <- HttpApiError.ensure(programSpecs.nonEmpty, HttpApiError.badRequest("At least one program is required."))
      _ <- HttpApiError.ensure(
        programSpecs.map(_.sourcePart.trim).distinct.size == programSpecs.size,
        HttpApiError.badRequest("Submission source part names must be unique.")
      )
      programs <- programSpecs.traverse { spec =>
        val sourcePart = spec.sourcePart.trim
        for
          _ <- HttpApiError.ensure(sourcePart.nonEmpty, HttpApiError.badRequest("Submission source part name is required."))
          source <- MultipartTextSupport.requireUtf8Text(multipart, sourcePart, SourceMaxBytes)
        yield spec.role -> CreateSubmissionRequest.Program(spec.language, SubmissionSourceCode(source))
      }
      _ <- HttpApiError.ensure(
        programs.map(_._1.trim).distinct.size == programs.size,
        HttpApiError.badRequest("Submission program roles must be unique.")
      )
    yield CreateSubmissionRequest(problemSlug, programs.toMap)
