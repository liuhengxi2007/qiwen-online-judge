package domains.contest.api

import cats.effect.IO
import domains.auth.api.AuthenticatedApi
import domains.auth.objects.internal.AuthenticatedUser
import domains.contest.objects.{ContestDescription, ContestSlug, ContestTitle}
import domains.contest.objects.request.UpdateContestRequest
import domains.contest.objects.response.{ContestDetail, ContestRegistrationStatus}
import domains.contest.table.contest.ContestTable
import domains.contest.utils.{ContestAccessPolicyValidation, ContestAccessRules}
import domains.usergroup.api.ListUserGroupSlugsForMember
import io.circe.Encoder
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.{Method, Request, Status}
import shared.api.{ApiMessages, ApiPath, HttpApiError, PathParams}

import java.sql.Connection

/** 更新比赛基础信息和访问策略的认证 API，只有比赛管理者可调用。 */
object UpdateContest extends AuthenticatedApi[(ContestSlug, UpdateContestRequest), ContestDetail]:

  override val method: Method = Method.POST
  override val path: ApiPath = ApiPath("/api/contests/:contestSlug/update")
  override val successStatus: Status = Status.Ok
  override protected val outputEncoder: Encoder[ContestDetail] = summon[Encoder[ContestDetail]]

  /** 从路径解析比赛 slug 并读取更新请求体。 */
  override def decode(request: Request[IO], pathParams: PathParams): IO[(ContestSlug, UpdateContestRequest)] =
    for
      contestSlug <- HttpApiError.fromEitherBadRequest(pathParams.require("contestSlug").flatMap(ContestSlug.parse))
      body <- request.as[UpdateContestRequest]
    yield (contestSlug, body)

  /** 校验管理权、标题描述、时间范围和授权主体后更新比赛并返回最新详情。 */
  override def plan(
    connection: Connection,
    actor: AuthenticatedUser,
    input: (ContestSlug, UpdateContestRequest)
  ): IO[ContestDetail] =
    val (contestSlug, request) = input
    for
      maybeContest <- ContestTable.findBySlug(connection, contestSlug)
      contest <- maybeContest match
        case Some(contest) => IO.pure(contest)
        case None => HttpApiError.raise(HttpApiError.notFound(ApiMessages.contestNotFound))
      actorGroupSlugs <- ListUserGroupSlugsForMember.plan(connection, actor.username)
      _ <- HttpApiError.ensure(
        ContestAccessRules.canManageContest(actor, contest, actorGroupSlugs.slugs.toSet),
        HttpApiError.forbidden(ApiMessages.contestManagerRequired)
      )
      title <- HttpApiError.fromEitherBadRequest(ContestTitle.parse(request.title.value))
      description <- HttpApiError.fromEitherBadRequest(ContestDescription.parse(request.description.value))
      sanitizedInput = request.copy(title = title, description = description)
      _ <- HttpApiError.ensure(sanitizedInput.endAt.isAfter(sanitizedInput.startAt), HttpApiError.badRequest("Contest end time must be after start time."))
      sanitizedRequest = ContestAccessPolicyValidation.sanitizePolicy(sanitizedInput)
      _ <- ContestAccessPolicyValidation.validateAccessPolicySubjects(connection, sanitizedRequest.accessPolicy)
      updatedContest <- ContestTable.update(connection, contest, sanitizedRequest)
      isRegistered <- ContestTable.isRegistered(connection, updatedContest.id, actor.username)
    yield ContestDetail.fromContest(
      updatedContest,
      if isRegistered then ContestRegistrationStatus.registered else ContestRegistrationStatus.notRegistered,
      canManage = true,
      includeProblems = true
    )
