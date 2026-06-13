package domains.contest.api

import cats.effect.IO
import domains.auth.api.AuthenticatedApi
import domains.auth.objects.internal.AuthenticatedUser
import domains.contest.objects.*
import domains.contest.objects.request.CreateContestRequest
import domains.contest.objects.response.{ContestDetail, ContestRegistrationStatus}
import domains.contest.table.contest.ContestTable
import domains.contest.utils.{ContestAccessPolicyValidation, ContestAccessRules}
import io.circe.Encoder
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.{Method, Request, Status}
import shared.api.{ApiMessages, ApiPath, HttpApiError, PathParams}

import java.sql.Connection

/** 创建比赛的认证 API，仅全局比赛管理员可调用，并负责校验时间范围与访问策略主体。 */
object CreateContest extends AuthenticatedApi[CreateContestRequest, ContestDetail]:

  override val method: Method = Method.POST
  override val path: ApiPath = ApiPath("/api/contests")
  override val successStatus: Status = Status.Created
  override protected val outputEncoder: Encoder[ContestDetail] = summon[Encoder[ContestDetail]]

  /** 读取创建比赛请求体，路径参数不参与该入口。 */
  override def decode(request: Request[IO], pathParams: PathParams): IO[CreateContestRequest] =
    val _ = pathParams
    request.as[CreateContestRequest]

  /** 规范化 slug/标题/描述，拒绝重复 slug 和无效授权主体，写库后返回可管理的比赛详情。 */
  override def plan(connection: Connection, actor: AuthenticatedUser, request: CreateContestRequest): IO[ContestDetail] =
    for
      _ <- HttpApiError.ensure(
        ContestAccessRules.canCreateContests(actor),
        HttpApiError.forbidden(ApiMessages.contestManagerRequired)
      )
      slug <- HttpApiError.fromEitherBadRequest(ContestSlug.parse(request.slug.value))
      title <- HttpApiError.fromEitherBadRequest(ContestTitle.parse(request.title.value))
      description <- HttpApiError.fromEitherBadRequest(ContestDescription.parse(request.description.value))
      validRequest = request.copy(slug = slug, title = title, description = description)
      _ <- HttpApiError.ensure(validRequest.endAt.isAfter(validRequest.startAt), HttpApiError.badRequest("Contest end time must be after start time."))
      existing <- ContestTable.findBySlug(connection, validRequest.slug)
      _ <- HttpApiError.ensure(existing.isEmpty, HttpApiError.conflict(ApiMessages.contestSlugExists))
      _ <- ContestAccessPolicyValidation.validateAccessPolicySubjects(connection, validRequest.accessPolicy)
      contest <- ContestTable.insert(connection, actor.username, ContestAccessPolicyValidation.sanitizePolicy(validRequest))
    yield ContestDetail.fromContest(contest, ContestRegistrationStatus.notRegistered, canManage = true, includeProblems = true)
