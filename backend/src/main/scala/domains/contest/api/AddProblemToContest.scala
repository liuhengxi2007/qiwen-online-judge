package domains.contest.api

import cats.effect.IO
import domains.auth.api.AuthenticatedApi
import domains.auth.objects.internal.AuthenticatedUser
import domains.contest.objects.ContestSlug
import domains.contest.objects.request.AddProblemToContestRequest
import domains.contest.objects.response.{ContestDetail, ContestRegistrationStatus}
import domains.contest.table.contest.ContestTable
import domains.contest.table.contest.ContestTable.AddProblemTableResult
import domains.contest.utils.ContestAccessRules
import domains.problem.api.EvaluateProblemAccess
import domains.usergroup.api.ListUserGroupSlugsForMember
import io.circe.Encoder
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.{Method, Request, Status}
import shared.api.{ApiMessages, ApiPath, HttpApiError, PathParams}

import java.sql.Connection

/** 将题目加入指定比赛的认证 API，要求调用者具备该比赛的管理权限以及题目的管理权限。 */
object AddProblemToContest extends AuthenticatedApi[(ContestSlug, AddProblemToContestRequest), ContestDetail]:

  override val method: Method = Method.POST
  override val path: ApiPath = ApiPath("/api/contests/:contestSlug/problems")
  override val successStatus: Status = Status.Ok
  override protected val outputEncoder: Encoder[ContestDetail] = summon[Encoder[ContestDetail]]

  /** 从路径读取比赛 slug 并解析请求体中的题目 slug，非法路径参数或请求体会转为 400。 */
  override def decode(request: Request[IO], pathParams: PathParams): IO[(ContestSlug, AddProblemToContestRequest)] =
    for
      contestSlug <- HttpApiError.fromEitherBadRequest(pathParams.require("contestSlug").flatMap(ContestSlug.parse))
      body <- request.as[AddProblemToContestRequest]
    yield (contestSlug, body)

  /** 校验比赛存在、调用者可管理比赛和题目后写入关联，成功返回包含题目列表的最新比赛详情。 */
  override def plan(
    connection: Connection,
    actor: AuthenticatedUser,
    input: (ContestSlug, AddProblemToContestRequest)
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
      problemAccess <- EvaluateProblemAccess.plan(connection, actor, request.problemSlug)
      /** 注意：题目不存在或调用者不能管理题目都返回 404，用于避免暴露不可管理题目的存在性。 */
      problem <- problemAccess.problem match
        case Some(problem) if problemAccess.canManage => IO.pure(problem)
        case _ => HttpApiError.raise(HttpApiError.notFound(ApiMessages.problemNotFound))
      _ <- ContestTable.addProblem(connection, contest.id, problem.id).flatMap {
        case AddProblemTableResult.Linked => IO.unit
        case AddProblemTableResult.AlreadyLinked =>
          HttpApiError.raise(HttpApiError.conflict(ApiMessages.problemAlreadyLinkedToContest))
      }
      updatedContest <- ContestTable.findBySlug(connection, contest.slug).flatMap {
        case Some(contest) => IO.pure(contest)
        case None => HttpApiError.raise(HttpApiError.notFound(ApiMessages.contestNotFound))
      }
      isRegistered <- ContestTable.isRegistered(connection, updatedContest.id, actor.username)
    yield ContestDetail.fromContest(
      updatedContest,
      if isRegistered then ContestRegistrationStatus.registered else ContestRegistrationStatus.notRegistered,
      canManage = true,
      includeProblems = true
    )
