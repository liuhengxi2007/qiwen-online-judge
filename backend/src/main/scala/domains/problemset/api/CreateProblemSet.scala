package domains.problemset.api

import cats.effect.IO
import domains.auth.api.AuthenticatedApi
import domains.auth.objects.internal.AuthenticatedUser
import domains.problemset.utils.ProblemSetAccessPolicyValidation

import domains.problemset.objects.*
import domains.problemset.objects.request.CreateProblemSetRequest
import domains.problemset.objects.response.ProblemSetDetail
import domains.problemset.utils.ProblemSetAccessRules
import domains.problemset.table.problem_set.ProblemSetTable
import io.circe.Encoder
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.{Method, Request, Status}
import shared.api.{ApiMessages, ApiPath, HttpApiError, PathParams}

import java.sql.Connection

/** 创建题单的认证 API，仅题目管理员可调用，并校验 slug 与访问策略。 */
object CreateProblemSet extends AuthenticatedApi[CreateProblemSetRequest, ProblemSetDetail]:

  override val method: Method = Method.POST
  override val path: ApiPath = ApiPath("/api/problem-sets")
  override val successStatus: Status = Status.Created
  override protected val outputEncoder: Encoder[ProblemSetDetail] = summon[Encoder[ProblemSetDetail]]

  /** 读取创建题单请求体，路径参数不参与该入口。 */
  override def decode(request: Request[IO], pathParams: PathParams): IO[CreateProblemSetRequest] =
    val _ = pathParams
    request.as[CreateProblemSetRequest]

  /** 规范化 slug/标题/描述，拒绝题单 slug 重复或与题目 slug 冲突后写入题单。 */
  override def plan(
    connection: Connection,
    actor: AuthenticatedUser,
    request: CreateProblemSetRequest
  ): IO[ProblemSetDetail] =
    for
      _ <- HttpApiError.ensure(
        ProblemSetAccessRules.canManageProblemSets(actor),
        HttpApiError.forbidden(ApiMessages.problemManagerRequired)
      )
      slug <- HttpApiError.fromEitherBadRequest(ProblemSetSlug.parse(request.slug.value))
      title <- HttpApiError.fromEitherBadRequest(ProblemSetTitle.parse(request.title.value))
      description <- HttpApiError.fromEitherBadRequest(ProblemSetDescription.parse(request.description.value))
      validRequest = request.copy(slug = slug, title = title, description = description)
      existing <- ProblemSetTable.findBySlug(connection, validRequest.slug)
      _ <- HttpApiError.ensure(existing.isEmpty, HttpApiError.conflict(ApiMessages.problemSetSlugExists))
      conflictingProblem <- ProblemSetAccessPolicyValidation.problemSlugExists(connection, validRequest.slug.value)
      _ <- HttpApiError.ensure(!conflictingProblem, HttpApiError.conflict(ApiMessages.problemSetSlugConflictsWithProblem))
      _ <- ProblemSetAccessPolicyValidation.validateAccessPolicySubjects(connection, validRequest.accessPolicy)
      problemSet <- ProblemSetTable.insert(connection, actor.username, ProblemSetAccessPolicyValidation.sanitizePolicy(validRequest))
    yield ProblemSetDetail.fromProblemSet(problemSet)
