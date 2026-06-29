package domains.contest.api

import cats.effect.IO
import domains.auth.api.AuthenticatedApi
import domains.auth.objects.internal.AuthenticatedUser
import domains.contest.objects.ContestSlug
import domains.contest.objects.response.{ContestDetail, ContestRegistrationStatus}
import domains.contest.table.contest.ContestTable
import domains.contest.utils.ContestAccessRules
import domains.problem.api.EvaluateProblemAccess
import domains.problem.objects.ProblemSlug
import domains.usergroup.api.ListUserGroupSlugsForMember
import io.circe.Encoder
import org.http4s.{Method, Request, Status}
import shared.api.{ApiMessages, ApiPath, HttpApiError, PathParams}

import java.sql.Connection

/** 从比赛移除题目的认证 API，只有比赛管理者可调用。 */
object RemoveProblemFromContest extends AuthenticatedApi[(ContestSlug, ProblemSlug), ContestDetail]:

  override val method: Method = Method.POST
  override val path: ApiPath = ApiPath("/api/contests/:contestSlug/problems/:problemSlug/delete")
  override val successStatus: Status = Status.Ok
  override protected val outputEncoder: Encoder[ContestDetail] = summon[Encoder[ContestDetail]]

  /** 从路径解析比赛 slug 和题目 slug，删除入口不读取请求体。 */
  override def decode(request: Request[IO], pathParams: PathParams): IO[(ContestSlug, ProblemSlug)] =
    val _ = request
    for
      contestSlug <- HttpApiError.fromEitherBadRequest(pathParams.require("contestSlug").flatMap(ContestSlug.parse))
      problemSlug <- HttpApiError.fromEitherBadRequest(pathParams.require("problemSlug").flatMap(ProblemSlug.parse))
    yield (contestSlug, problemSlug)

  /** 校验比赛管理权后删除赛题关联，并返回更新后的比赛详情。 */
  override def plan(
    connection: Connection,
    actor: AuthenticatedUser,
    input: (ContestSlug, ProblemSlug)
  ): IO[ContestDetail] =
    val (contestSlug, problemSlug) = input
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
      problemAccess <- EvaluateProblemAccess.plan(connection, actor, problemSlug)
      problem <- problemAccess.problem match
        case Some(problem) => IO.pure(problem)
        case None => HttpApiError.raise(HttpApiError.notFound(ApiMessages.problemNotFound))
      removed <- ContestTable.removeProblem(connection, contest.id, problem.id)
      _ <- HttpApiError.ensure(removed, HttpApiError.notFound(ApiMessages.contestProblemNotFound))
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
