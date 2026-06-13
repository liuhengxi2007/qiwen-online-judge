package domains.problem.api

import cats.effect.IO
import cats.syntax.all.*
import domains.auth.api.AuthenticatedApi
import domains.auth.objects.internal.AuthenticatedUser
import domains.judge.utils.JudgeTaskBuilder
import domains.problem.utils.ProblemDataStorage
import domains.problem.utils.ProblemDataApiHelpers

import domains.problem.objects.{ProblemDataPath, ProblemSlug}
import domains.problem.objects.internal.ProblemDataManifestEntry
import domains.problem.objects.response.ProblemDetail
import domains.problem.table.problem.ProblemDataStateTable
import domains.problem.table.problem_data_file.ProblemDataFileTable
import domains.submission.objects.SubmissionResultDisplayMode
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.deriveDecoder
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.{Method, Request, Status}
import shared.api.{ApiPath, HttpApiError, PathParams}

import java.sql.Connection
import java.time.Instant

/** 设置题目数据 ready 状态的请求体；true 会触发 judge.yaml 完整校验，false 只下线数据。 */
final case class SetProblemReadyRequest(ready: Boolean)

/** 题目数据 ready 状态管理 API；校验题目数据可判、清理冗余文件并更新结果展示模式。 */
final case class SetProblemDataReady(problemDataStorage: ProblemDataStorage)
    extends AuthenticatedApi[(ProblemManagementContext, SetProblemReadyRequest), ProblemDetail]:

  private given Decoder[SetProblemReadyRequest] = deriveDecoder[SetProblemReadyRequest]

  override val method: Method = Method.POST
  override val path: ApiPath = ApiPath("/api/problems/:problemSlug/data/ready-state")
  override val successStatus: Status = Status.Ok
  override protected val outputEncoder: Encoder[ProblemDetail] = summon[Encoder[ProblemDetail]]

  /** 解析题目管理上下文和 ready 布尔值。 */
  override def decode(request: Request[IO], pathParams: PathParams): IO[(ProblemManagementContext, SetProblemReadyRequest)] =
    for
      context <- ProblemManagementContext.decode(request, pathParams)
      body <- request.as[SetProblemReadyRequest]
    yield (context, body)

  /** 校验管理权限后设置 ready 状态；ready=true 会读取对象存储中的 judge.yaml 和清单。 */
  override def plan(
    connection: Connection,
    actor: AuthenticatedUser,
    input: (ProblemManagementContext, SetProblemReadyRequest)
  ): IO[ProblemDetail] =
    val (context, request) = input
    ProblemManagementContext
      .requireManagedProblem(connection, actor, context)
      .flatMap(_ => setManagedProblemDataReady(connection, context.problemSlug, request.ready))

  /** 在题目行锁内切换 ready 状态，输出刷新后的题目详情。 */
  def setManagedProblemDataReady(connection: Connection, problemSlug: ProblemSlug, ready: Boolean): IO[ProblemDetail] =
    ProblemDataApiHelpers.withProblemForUpdate(connection, problemSlug) { problem =>
      if ready then markProblemReady(connection, problem)
      else markProblemNotReady(connection, problem)
    }

  private def markProblemNotReady(connection: Connection, problem: ProblemDetail): IO[ProblemDetail] =
    ProblemDataStateTable
      .updateDataReady(connection, problem.id, Instant.now(), problem.data.value, ready = false)
      .flatMap(_ => ProblemDataApiHelpers.refreshedManagedProblem(connection, problem, "Problem disappeared after ready update."))

  private def markProblemReady(
    connection: Connection,
    problem: ProblemDetail
  ): IO[ProblemDetail] =
    val judgeYamlPath = ProblemDataPath("judge.yaml")
    for
      manifest <- ProblemDataFileTable.manifestForProblem(connection, problem.id, problem.slug)
      maybeConfig <- problemDataStorage.readPath(problem.slug, judgeYamlPath)
      result <- maybeConfig match
        case None =>
          HttpApiError.raise(HttpApiError.badRequest("judge.yaml is required at the problem data root."))
        case Some((_, bytes)) =>
          JudgeTaskBuilder
            .validateReadyConfigBytes(bytes, problem, manifest)
            .fold(
              message => HttpApiError.raise(HttpApiError.badRequest(message)),
              validation => retainReadyFiles(connection, problem, manifest.entries, validation.retainedPaths, validation.resultDisplayMode)
            )
    yield result

  private def retainReadyFiles(
    connection: Connection,
    problem: ProblemDetail,
    entries: List[ProblemDataManifestEntry],
    retainedPaths: Set[ProblemDataPath],
    resultDisplayMode: SubmissionResultDisplayMode
  ): IO[ProblemDetail] =
    val retainedEntries = entries.filter(entry => retainedPaths.contains(entry.path))
    val redundantPaths = entries.map(_.path).filterNot(retainedPaths.contains)
    for
      snapshot <- problemDataStorage.snapshotDirectory(problem.slug)
      updatedProblem <- redundantPaths
        .traverse_(path => problemDataStorage.deletePath(problem.slug, path).void)
        .flatMap(_ => ProblemDataFileTable.deleteExceptPaths(connection, problem.id, retainedPaths))
        .flatMap(_ =>
          ProblemDataStateTable.updateDataReady(
            connection,
            problem.id,
            Instant.now(),
            ProblemDataApiHelpers.summaryFilenameForEntries(retainedEntries),
            ready = true,
            resultDisplayMode = resultDisplayMode
          )
        )
        .flatMap(_ => ProblemDataApiHelpers.refreshedManagedProblem(connection, problem, "Problem disappeared after ready update."))
        .handleErrorWith { error =>
          problemDataStorage.restoreDirectory(problem.slug, snapshot) *> IO.raiseError(error)
        }
    yield updatedProblem
